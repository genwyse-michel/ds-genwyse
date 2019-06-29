package com.genwyse.docushare;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.xerox.docushare.DSAclEntry;
import com.xerox.docushare.DSAuthorizationException;
import com.xerox.docushare.DSContentElement;
import com.xerox.docushare.DSException;
import com.xerox.docushare.DSFactory;
import com.xerox.docushare.DSHandle;
import com.xerox.docushare.DSInvalidLicenseException;
import com.xerox.docushare.DSLoginPrincipal;
import com.xerox.docushare.DSObject;
import com.xerox.docushare.DSObjectIterator;
import com.xerox.docushare.DSPermission;
import com.xerox.docushare.DSServer;
import com.xerox.docushare.DSSession;
import com.xerox.docushare.object.DSCollection;
import com.xerox.docushare.object.DSDocument;
import com.xerox.docushare.object.DSGroup;
import com.xerox.docushare.object.DSRendition;
import com.xerox.docushare.object.DSVersion;
import com.xerox.docushare.property.DSPropDesc;
import com.xerox.docushare.property.DSPropSet;
import com.xerox.docushare.property.DSPropTypes;
import com.xerox.docushare.property.DSProperties;

public class DSCopier {
  
  public static class AclCalculation {
    public enum Operation {
      CopySource,
      CopyDest,
      Inter,
      Union;
    };
    private Operation principalOperation;
    private Operation rightsOperation;
    public static final AclCalculation destinationAcl = new AclCalculation(Operation.CopyDest, Operation.CopyDest);
    public static final AclCalculation sourceAcl = new AclCalculation(Operation.Union, Operation.Union);
    public static final AclCalculation mergeAcl = new AclCalculation(Operation.Union, Operation.Union);

    public AclCalculation(Operation principalOperation, Operation rightsOperation) {
      this.principalOperation = principalOperation;
      this.rightsOperation = rightsOperation;
    }

    public Operation getPrincipalOperation() {
      return principalOperation;
    }

    public Operation getRightsOperation() {
      return rightsOperation;
    }
    
    public String toString() {
      return "[AclCalculation: principal="+principalOperation.toString()+",rights="+rightsOperation.toString()+"]";
    }
  };
  
  public static class CopyOptions {
    boolean deepCopy = true;
    DSLoginPrincipal owner = null;
    boolean allDocsVersions = false;
    boolean allDocsRenditions = false;
    boolean forceRO = false;
    AclCalculation aclCalculation = AclCalculation.mergeAcl;
    DSAclEntry[] globalACL = null;
    AclCalculation globalAclCalculation = AclCalculation.mergeAcl;

    public boolean isDeepCopy() {
      return deepCopy;
    }
    public void setDeepCopy(boolean deepCopy) {
      this.deepCopy = deepCopy;
    }
    public DSLoginPrincipal getOwner() {
      return owner;
    }
    public void setOwner(DSLoginPrincipal owner) {
      this.owner = owner;
    }
    public boolean isAllDocsVersions() {
      return allDocsVersions;
    }
    public void setAllDocsVersions(boolean allDocsVersions) {
      this.allDocsVersions = allDocsVersions;
    }
    public boolean isAllDocsRenditions() {
      return allDocsRenditions;
    }
    public void setAllDocsRenditions(boolean allDocsRenditions) {
      this.allDocsRenditions = allDocsRenditions;
    }
    public boolean isForceRO() {
      return forceRO;
    }
    public void setForceRO(boolean forceRO) {
      this.forceRO = forceRO;
    }
    public AclCalculation getAclCalculation() {
      return aclCalculation;
    }
    public void setAclCalculation(AclCalculation aclCalculation) {
      this.aclCalculation = aclCalculation;
    }
    public DSAclEntry[] getGlobalACL() {
      return globalACL;
    }
    public void setGlobalACL(DSAclEntry[] globalACL) {
      this.globalACL = globalACL;
    }
    public AclCalculation getGlobalAclCalculation() {
      return globalAclCalculation;
    }
    public void setGlobalAclCalculation(AclCalculation globalAclCalculation) {
      this.globalAclCalculation = globalAclCalculation;
    }
  };
  
  private static Logger logger = Logger.getLogger(DSCopier.class);

  // Propriétés à ne pas modifier même si elle ne sont pas R/O pour DocuShare
  private final static Map<String,Boolean> skipProperties = new HashMap<String,Boolean> ();
  {
    skipProperties.put("isRecord", true);
  }

  
  protected Map<String,DSObject> createdObjects = new HashMap<String,DSObject>(); // Pour éviter les boucles: ancien handle=> nouvel objet
  protected DSSession dsSession = null;
  CopyOptions copyOptions = null;
  
  public DSCopier (DSSession ds_session)
  {
    dsSession = ds_session;
    copyOptions = new CopyOptions();
  }
  
  public DSCopier (DSSession ds_session, CopyOptions options)
  {
    dsSession = ds_session;
    copyOptions = options;
  }
  
  public CopyOptions getCopyOptions ()
  {
    return copyOptions;
  }
  
  /*
   * Copie d'un objet vers une collection, avec possibilité de forcer les attributs de la copie.
   * En cas de copie d'arbre, on tient compte des cycles.
   * 
   * Options (voir CopyOptions) :
   *   - copie 1er niveau seulement ou d'arbre (deepCopy)
   *   - force (si autorisé) le propriétaire de la copie
   *   - force l'état lecture seule
   *   - copie seulement la version (resp. rendition) préférée, ou toutes les versions (resp. renditions).
   */
  
  public DSObject copy (
      DSObject obj, 
      DSCollection dest, 
      DSPropSet field_values) throws DSException
  {
    return copy(obj,dest,field_values,null);
  }
  
  /*
   * Copie d'un sous-arbre. On tient compte des boucles.
   * 
   *   obj: objet à copier
   *   dest: collection destination où créer la copie
   *   field_values: valeurs des propriétés pour l'objet copie
   *   generic_values: valeurs de propriétés héritables pour l'objet copie et son contenu
   *   mode
   */
  public DSObject copy (
      DSObject obj,
      DSCollection dest, 
      DSPropSet field_values,
      Map<String,Object> generic_values
      ) throws DSException
  {
    return copy(obj, dest, field_values, generic_values, dest.getAclEntries());
  }
  
  public DSObject copy (
      DSObject obj,
      DSCollection dest, 
      DSPropSet field_values,
      Map<String,Object> generic_values,
      DSAclEntry[] context_acl
      ) throws DSException
  {
    if (obj==null) return null;
    String src_handle = obj.getHandle().toString();
    DSObject result = createdObjects.get(src_handle);
    if (result!=null) {
      // L'objet a d&jà été copié, on le place dans la collection
      if (dest!=null) {
        dest.addChild(result);
      }
    }
    
    else if (obj.isTypeOf("Collection")) {
      DSCollection coll = (DSCollection) obj;
      result = copy (coll, dest, field_values, generic_values, context_acl);
      createdObjects.put(src_handle, result);
    }
    else if (obj.isTypeOf("Document")) {
      // Copie de document (dernière version Docushare seulement)
      DSDocument doc = (DSDocument) obj;
      result = copy(doc, dest, field_values, generic_values, context_acl);
      createdObjects.put(src_handle, result);
    }
    else {
      // Autres types d'objets éventuels
      result = copyObject(obj, dest, null, field_values, generic_values);
      createdObjects.put(src_handle, result);
    }
    return result;
  }

  /*
   * Calcul des propriétés d'un objet à créer/mettre à jour selon
   *   - les propriétés initiales, copiées d'après le modèle (props)
   *   - des valeurs d'index communes à l'arborescence et héritables (generic_values)
   *   - les valeurs d'index à fixer pour l'objet cible (field_values).
   *   
   *   Les valeurs communes peuvent être utilisées dans des expressions définies
   *   par les propriétés du modèle.
   */
  private void setNewFieldValues (DSProperties props, DSPropSet field_values, Map<String,Object> generic_values) throws DSException
  {
    if (generic_values!=null) {
      for (Entry<String,Object> entry: generic_values.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        props.setPropValue(key, value);
      }
    }
    if (field_values != null) {
      DSPropDesc[] prop_descs = field_values.getPropDescs();
      for (int i=0; i<prop_descs.length; i++) {
        String prop_name = prop_descs[i].getName();
        Object prop_value = field_values.getPropValue(prop_name);
        props.setPropValue(prop_name, prop_value);
      }
    }
    for (String prop_name: props.getPropNames()) {
      Object prop_value = props.getPropValue(prop_name);
      if (prop_value instanceof String) {
        // Remplacement de valeurs ?
        String prop_string = (String) prop_value;
        boolean replacement_done = false;
        int param_pos;
        while ((param_pos = prop_string.indexOf('{'))>=0) {
          int param_end = prop_string.indexOf('}', param_pos);
          if (param_end>param_pos+1) {
            String param_name = prop_string.substring(param_pos+1, param_end);
            if (generic_values.containsKey(param_name)) {
              prop_string = prop_string.substring(0, param_pos)+generic_values.get(param_name)+prop_string.substring(param_end+1);
              replacement_done = true;
            }
          }
        }
        if (replacement_done) {
          props.setPropValue(prop_name, prop_string);
        }
      }
    }
  }
  
  protected DSCollection copy (DSCollection coll, DSCollection dest, DSPropSet field_values, Map<String,Object> generic_values, DSAclEntry[] context_acl)
  {
    String mess_err = "Erreur en copiant une collection";
    try {
      if (context_acl==null) {
        context_acl = dest.getAclEntries();
      }
      DSAclEntry[] model_acl = coll.getAclEntries();
      DSAclEntry[] new_coll_ACL = prepareACL(model_acl, context_acl);
      DSProperties coll_props = coll.toProperties(true); 
      setNewFieldValues (coll_props, field_values, generic_values);
      
      DSHandle new_coll_handle = dsSession.createObject(coll_props, "containment", dest, copyOptions.owner, new_coll_ACL);
      DSCollection new_coll = (DSCollection) dsSession.getObject(new_coll_handle);
      
      //String src_handle = coll.getHandle().toString();
      //createdObjects.put(src_handle, new_coll);
      
      // Recherche et copie des objets inclus.
      if (copyOptions.isDeepCopy()) {
        DSObjectIterator coll_contents = coll.children(null);
        while (coll_contents.hasNext()) {
          DSObject obj = coll_contents.nextObject();
          if (copyOptions.deepCopy) {
            copy(obj, new_coll, null, generic_values);
          }
          else {
            new_coll.addChild(obj);
          }
        }
      }
      return new_coll;

    } catch (Exception e) {
      logger.error(mess_err, e);
    }

    return null;  
  }
  
  protected DSDocument copy (DSDocument doc, DSCollection dest, DSPropSet field_values, Map<String,Object> generic_values)
  {
    String mess_err = "Erreur en copiant un document";
    try {
      DSDocument new_doc = null;
      
      DSAclEntry[] new_doc_ACL = prepareACL(doc.getAclEntries(), dest.getAclEntries());
      DSProperties doc_props = doc.toProperties();
      setNewFieldValues (doc_props, field_values, generic_values);

      DSObjectIterator vers_iter = null, rend_iter = null;
      DSProperties vers_props = null, rend_props = null;
      DSVersion ds_vers, ds_pref_vers;
      DSRendition ds_rend, ds_pref_rend;
      
      if (copyOptions.allDocsVersions) {
        vers_iter = doc.getVersions(null);
        ds_vers = (DSVersion) vers_iter.next();
      }
      else {
        ds_vers =  doc.getPreferredVersion(null);
      }
      vers_props = ds_vers.toProperties();
       
      if (copyOptions.allDocsRenditions) {
        rend_iter = ds_vers.getRenditions(null);
        ds_rend = (DSRendition) rend_iter.next();
      }
      else {
        ds_rend = ds_vers.getPreferredRendition(null);
      }
      rend_props = ds_rend.toProperties();
      
      DSContentElement[] content_elts = ds_rend.getContentElements();
      String content_type = null;
      
      // Création du document avec une première version / rendition
      String link_type = "containment";
      DSHandle new_doc_handle = dsSession.createDocument(doc_props, vers_props, rend_props, content_elts, content_type, link_type, dest, copyOptions.owner, new_doc_ACL);
      new_doc = (DSDocument) dsSession.getObject(new_doc_handle);

      if (copyOptions.allDocsRenditions) {
        ds_pref_rend = ds_vers.getPreferredRendition(null);
        DSVersion new_version = (DSVersion) new_doc.getVersions(null).nextObject();
        while (rend_iter.hasNext()) {
          ds_rend = (DSRendition) rend_iter.nextObject();
          DSProperties other_rend_props = ds_rend.toProperties();
          DSContentElement[] other_rend_content_elts = ds_rend.getContentElements();
          DSHandle new_rend_handle = new_version.addRendition(other_rend_props, other_rend_content_elts);
          if (ds_rend.equals(ds_pref_rend)) {
            new_version.setPreferredRendition((DSRendition) dsSession.getObject(new_rend_handle));
          }
        }
      }
      
      if (copyOptions.allDocsVersions)
      {
        ds_pref_vers = doc.getPreferredVersion(null);
        while (vers_iter.hasNext()) {
          ds_vers = (DSVersion) vers_iter.next();
          vers_props = ds_vers.toProperties();
          
          if (copyOptions.allDocsRenditions) {
            rend_iter = ds_vers.getRenditions(null);
            ds_rend = (DSRendition) rend_iter.next();
          }
          else {
            ds_rend = ds_vers.getPreferredRendition(null);
          }
          rend_props = ds_rend.toProperties();
          
          content_elts = ds_rend.getContentElements();
          
          DSHandle new_vers_handle = new_doc.addVersion(vers_props, rend_props, content_elts);
          if (ds_vers.equals(ds_pref_vers)) {
            new_doc.setPreferredVersion((DSVersion) dsSession.getObject(new_vers_handle));
          }
          if (copyOptions.allDocsRenditions) {
            ds_pref_rend = ds_vers.getPreferredRendition(null);
            DSVersion new_version = (DSVersion) dsSession.getObject(new_vers_handle);
            while (rend_iter.hasNext()) {
              ds_rend = (DSRendition) rend_iter.nextObject();
              DSProperties other_rend_props = ds_rend.toProperties();
              DSContentElement[] other_rend_content_elts = ds_rend.getContentElements();
              DSHandle new_rend_handle = new_version.addRendition(other_rend_props, other_rend_content_elts);
              if (ds_rend.equals(ds_pref_rend)) {
                new_version.setPreferredRendition((DSRendition) dsSession.getObject(new_rend_handle));
              }
            }
          }
        }
      }
      
      /*
        // Seulement la version et rendition préférées

        DSAclEntry[] new_version_ACL = copyACL(ds_pref_version.getAclEntries(), copyOptions.forceRO);
        DSProperties version_props = ds_pref_version.toProperties();
        
        DSRendition ds_rend = (DSRendition) ds_pref_version.getPreferredRendition(null);
        DSProperties rendition_props = ds_rend.toProperties();
        
        DSContentElement[] content_elts = ds_rend.getContentElements();
        String content_type = null;
        String link_type = "containment";
        
        DSHandle new_doc_handle = dsSession.createDocument(doc_props, version_props, rendition_props, content_elts, content_type, link_type, dest, copyOptions.owner, new_doc_ACL);
        new_doc = (DSDocument) dsSession.getObject(new_doc_handle);
      */
      return new_doc;
    } catch (DSAuthorizationException e) {
      logger.error(mess_err, e);
    } catch (DSException e) {
      logger.error(mess_err, e);
    } catch (Exception e) {
      logger.error(mess_err, e);
    }

    return null;  
  }
  
  DSObject copyObject(DSObject obj, DSCollection dest, DSLoginPrincipal owner, DSPropSet field_values, Map<String,Object> generic_values) 
  {
    // Création d'une copie d'un objet (hors cas Document)
    // L'objet est copié et mis en lecture seule
    String mess_err = "Erreur en copiant un objet";
    try {
      DSAclEntry[] new_obj_ACL = prepareACL(obj.getAclEntries(), dest.getAclEntries());
      DSProperties obj_props = obj.toProperties(); 
      setNewFieldValues (obj_props, field_values, generic_values);

      DSHandle new_obj_handle = dsSession.createObject(obj_props, "containment", dest, owner, new_obj_ACL);
      DSObject new_object = dsSession.getObject(new_obj_handle);
      return new_object;
    } catch (DSAuthorizationException e) {
      logger.error(mess_err, e);
    } catch (DSException e) {
      logger.error(mess_err, e);
    } catch (Exception e) {
      logger.error(mess_err, e);
    }
    return null;
  }
  
  /*
   * Mise à jour d'un objet (propriétés, droits) ayant été créé depuis un modèle, en tenant compte
   * des valeurs actuelles du modèle, des propriétés génériques et du contexte de droits.
   * 
   * model: objet modèle (propriétés et droits)
   * generic_values: valeurs de propriétés communes
   * parent: collection parente à prendre en compte pour le calcul des droits.
   */
  public boolean updateWithModel(DSObject model, DSObject obj, Map<String,Object> generic_values, DSAclEntry[] parent_acl, boolean cascade) throws DSAuthorizationException, DSException {
    // 
    DSProperties obj_old_props = obj.toProperties(); 
    DSProperties obj_new_props = model.toProperties();
    setNewFieldValues (obj_new_props, null, generic_values);
    boolean obj_changed = false;
    for (String prop_name: obj_old_props.getPropNames()) {
      DSPropDesc prop_desc = obj_old_props.getPropDesc(prop_name);
      if (!prop_desc.isReadOnly() &&!skipProperties.containsKey(prop_name)) {
        Object old_value = obj_old_props.getPropValue(prop_name);
        Object new_value = obj_new_props.getPropValue(prop_name);
        if ((old_value==null&&new_value!=null) ||(old_value!=null&&!old_value.equals(new_value))) {
          // La valeur actuelle n'est pas la valeur souhaitée.
          
          // On ne change pas la valeur quand la valeur actuelle n'est pas définie (null) et que la 
          // valeur souhaitée est la valeur par défaut
          Object default_value = prop_desc.getDefaultValue();
          if (old_value!=null || !new_value.equals(default_value)) {
            // Attention, dans le cas des props menu, si on efface la valeur on a une chaine
            // vide dans la valeur souhaitée, mais cette chaine n'est pas acceptée par DocuShare
            // pour un menu (exception), il faut mettre null.
            if ("".equals(new_value) && prop_desc.getType()==DSPropTypes.MENU) {
              new_value = null;
            }
            obj.set(prop_name, new_value);
            obj_changed = true;
          }
        }
      }
    }
    if (obj_changed) {
      obj.save();
    }
    
    // Mise à jour des ACL
    DSAclEntry[] new_obj_ACL = prepareACL(model.getAclEntries(), parent_acl);
    DSAclEntry[] old_obj_ACL = obj.getAclEntries();
    DSLoginPrincipal owner = obj.getOwner();
    
    boolean acl_changed = false;
    for (DSAclEntry old_acl_entry: old_obj_ACL) {
      boolean found = false;
      // Si le principal est le propriétaire ou un administrateur de contenu, le droit ne peut être
      // modifié, inutile de le comparer
      DSHandle principal = old_acl_entry.principal;
      if (owner.getHandle().equals(principal)) {
        found = true;
      }
      else if (principal.equals(DSGroup.content_admin_handle)) {
        found = true;
      }
      else for (DSAclEntry new_acl_entry: new_obj_ACL) {
        if (old_acl_entry.principal.equals(new_acl_entry.principal)) {
          found = true;
          if (old_acl_entry.permissions!=new_acl_entry.permissions) {
            acl_changed = true;
          }
          break;
        }
      }
      if (!found) {
        acl_changed = true;
      }
      if (acl_changed) break;
    }
    if (!acl_changed) {
      for (DSAclEntry new_acl_entry: new_obj_ACL) {
        boolean found = false;
        for (DSAclEntry old_acl_entry: old_obj_ACL) {
          if (old_acl_entry.principal.equals(new_acl_entry.principal)) {
            found = true;
            break;
          }
        }
        if (!found) {
          acl_changed = true;
          break;
        }
      }
    }
    
    boolean background_acl_change = false;
    if (acl_changed) {
      if (background_acl_change) {
        obj.replaceAclEntriesBackground(new_obj_ACL, cascade, true, "Remplacement des ACL de "+obj.getHandle());
      }
      else {
        obj.replaceAclEntries(new_obj_ACL, cascade, true);
      }
    }
    return obj_changed||acl_changed;
  }
  
  /*
   * Fusion de deux liste de droits.
   * 
   * On ne duplique pas une entrée pour un principal: si l'entrée existe, elle est fusionnée ou écrasée
   * par les droits à ajouter.
   * 
   * Les droits à ajouter peuvent être forcés en lecture seule.
   * 
   * On calcule d'une part la liste de principals concernés, puis pour chacun d'eux les droits selon
   * le paramétrage de la copie.
   */
  private DSAclEntry[] mergeACL(DSAclEntry[] src_acl, DSAclEntry[] dst_acl, boolean src_ro, AclCalculation acl_calculation) {
    int perms_ro = DSPermission.Search | 
        DSPermission.ReadObject |
        DSPermission.ReadLinked |
        DSPermission.ReadHistory;
    List<DSHandle> result_principals = new LinkedList<DSHandle> ();
    if (dst_acl==null) {
      dst_acl = new DSAclEntry[0];
    }
    if (src_acl==null) {
      src_acl = new DSAclEntry[0];
    }
    
    AclCalculation.Operation principal_operation = acl_calculation.getPrincipalOperation();
    AclCalculation.Operation right_operation = acl_calculation.getRightsOperation();

    // Calcul des principals ayant des droits
    // ======================================
    // On ajoute les principals destination s'ils sont nécessaires
    if (principal_operation==AclCalculation.Operation.CopyDest||
        principal_operation==AclCalculation.Operation.Union) {
      for (DSAclEntry dst_rights: dst_acl) {
        result_principals.add(dst_rights.principal);
      }
    }
    // Ajout des principals source s'ils sont nécessaires (ne pas dupliquer)
    if (principal_operation==AclCalculation.Operation.CopySource||
        principal_operation==AclCalculation.Operation.Union) {
      for (DSAclEntry src_rights: src_acl) {
        DSHandle principal = src_rights.principal;
      
        // existe-t-il déjà ?
        boolean principal_exists = false;
        for (DSHandle dst_principal: result_principals) {
          if (dst_principal.equals(principal)) {
            principal_exists = true;
            break;
          }
        }
        if (!principal_exists) {
          // On ajoute car il n'existe pas
          result_principals.add(src_rights.principal);
        }
      }
    }
    
    // Pour une intersection il ne faut que les principals qui sont dans les 2 acl
    if (principal_operation==AclCalculation.Operation.Inter) {
      for (DSAclEntry dst_rights: dst_acl) {
        for (DSAclEntry src_rights: src_acl) {
          if (dst_rights.principal.equals(src_rights.principal)) {
            // présent dans les deux listes
            result_principals.add(src_rights.principal);
            break;
          }
        }
      }
    }

    // Pour chacun des principals ayant des droit, on calcule ces droits selon
    // les droits de la collection destination, les droits de la collection copiée,
    // et la méthode de calcul.
    DSAclEntry[] results = new DSAclEntry[result_principals.size()];
    int num_entry = 0;
    for (DSHandle principal: result_principals) {
      // Recherche des droits à fusionner pour ce principal
      int dst_perms = 0;
      int src_perms = 0;
      
      for (DSAclEntry dst_rights: dst_acl) {
        if (dst_rights.principal.equals(principal)) {
          dst_perms = dst_rights.permissions;
          break;
        }
      }
      for (DSAclEntry src_rights: src_acl) {
        if (src_rights.principal.equals(principal)) {
          src_perms = src_rights.permissions;
          break;
        }
      }
      
      // Calcul
      int ac_perms;
      switch (right_operation) {
      case CopyDest:
        ac_perms = dst_perms;
        break;
      case CopySource:
        ac_perms = src_perms;
        break;
      case Inter:
        ac_perms = dst_perms & src_perms;
        break;
      case Union:
        ac_perms = dst_perms | src_perms;
        break;
      default:
        ac_perms = 0;
          
      }
      
      results[num_entry++] = new DSAclEntry(principal,ac_perms); 
    }

    return results;
  }
  
  // Prépare la liste de droits suivant le paramétrage de la copie et les droits de l'objet
  // à copier et de la collection destination.
  //
  //  Possibilité de:
  //    partir d'une liste de droits initiale (globalACL)
  //    forcer en r/o les droits sources
  DSAclEntry[] prepareACL(DSAclEntry[] src_acl, DSAclEntry[] dest_acl) {
    DSAclEntry[] target_acl = mergeACL(copyOptions.getGlobalACL(), dest_acl, false, copyOptions.getGlobalAclCalculation());
    DSAclEntry[] result_acl = mergeACL(src_acl, target_acl, copyOptions.isForceRO(), copyOptions.getAclCalculation());
    return result_acl;
  }

  public void clearCopiedObjects() {
    this.createdObjects.clear();
  }

  public static void main(String[] args) {
    // Pour les tests unitaires
    int num_arg = 0;
    String ds_domain = args[num_arg++];
    String ds_user = args[num_arg++];
    String ds_pwd = args[num_arg++];
    String src_id = null;
    String dest_id = null;
    DSSession ds_session = null;
    try {
      DSServer ds_server = DSFactory.createServer("localhost",1099);
      ds_session = ds_server.createSession( ds_domain, ds_user, ds_pwd);
      DSCopier copier = new DSCopier(ds_session);
      for (; num_arg<args.length; num_arg++) {
        String [] p = args[num_arg].split("=");
        String pn = p[0];
        if ("src".equals(pn)) src_id = p[1];
        else if ("dest".equals(pn)) dest_id = p[1];
        else if ("owner".equals(pn)) {
          DSLoginPrincipal owner = (DSLoginPrincipal) ds_session.getObject(new DSHandle(p[1]));
          copier.getCopyOptions().owner = owner;
        }
        else if ("deep".equals(pn)) {
          copier.getCopyOptions().deepCopy = Boolean.parseBoolean(p[1]);
        }
        else if ("all-versions".equals(pn)) {
          copier.getCopyOptions().allDocsVersions = true;
        }
        else if ("all-revisions".equals(pn)) {
          copier.getCopyOptions().allDocsRenditions = true;
        }
      }

      DSObject obj  = ds_session.getObject(new DSHandle(src_id));
      DSCollection dest = (DSCollection) ds_session.getObject(new DSHandle(dest_id));

      DSPropSet field_values = new DSPropSet();
      field_values.addPropVal(obj.getDSClass().getPropDesc("title"), "Objet copié");
      field_values.addPropVal(obj.getDSClass().getPropDesc("summary"), "Sommaire copié");
      
      copier.copy(obj, dest, field_values);
    } catch (DSInvalidLicenseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (DSException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      if (ds_session!=null) {
        try {
          ds_session.close();
        } catch (DSException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
      }
    }
  }
}
