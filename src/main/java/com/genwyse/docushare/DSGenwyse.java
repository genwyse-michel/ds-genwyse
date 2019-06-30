package com.genwyse.docushare;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.xerox.docushare.DSAclEntry;
import com.xerox.docushare.DSAuthorizationException;
import com.xerox.docushare.DSClass;
import com.xerox.docushare.DSContentElement;
import com.xerox.docushare.DSException;
import com.xerox.docushare.DSHandle;
import com.xerox.docushare.DSLinkItem;
import com.xerox.docushare.DSLinkIterator;
import com.xerox.docushare.DSLoginPrincipal;
import com.xerox.docushare.DSObject;
import com.xerox.docushare.DSObjectIterator;
import com.xerox.docushare.DSSelectSet;
import com.xerox.docushare.DSSession;
import com.xerox.docushare.FileContentElement;
import com.xerox.docushare.object.DSCollection;
import com.xerox.docushare.object.DSDocument;
import com.xerox.docushare.object.DSRendition;
import com.xerox.docushare.object.DSVersion;
import com.xerox.docushare.property.DSLinkDesc;
import com.xerox.docushare.property.DSPropDesc;
import com.xerox.docushare.property.DSPropTypes;
import com.xerox.docushare.property.DSProperties;

public class DSGenwyse {
  private static final Logger logger = Logger.getLogger(DSGenwyse.class);
  
  public static final String classProp = "class";

  public class DSAncestry extends LinkedList<DSObject> {

    /**
     * 
     */
    private static final long serialVersionUID = 1550348642586399795L;
    
  }

  public static void moveObject (DSObject folder, DSCollection from, DSCollection to) throws DSAuthorizationException, DSException
  {
    DSObjectIterator src_parent_iter = folder.getParents(null);
    while (src_parent_iter.hasNext()) {
      DSCollection parent_coll = (DSCollection) src_parent_iter.nextObject();
      if (from==null || parent_coll.equals(from)) {
        parent_coll.removeChild(folder);
      }
    }
    to.addChild(folder);
  }
  
  /*
   * Recherche les chemins entre une collection racine et un objet
   */
  static class DSAncestryNode {
    DSObject object;
    List<DSAncestryNode> children = new LinkedList<DSAncestryNode> ();
    int numRun = 0; // pour la suppression des boucles lors du parcours
    
    public DSAncestryNode (DSObject obj) {
      object = obj;
    }
  }
  
  public static class DSAncestries extends LinkedList<DSObject[]> {

    /**
     * 
     */
    private static final long serialVersionUID = -2890115235751791486L;
    
  }
  
  public static DSAncestries getAncestries (DSSession dssession, DSObject object, DSCollection root_collection, boolean all_paths) throws DSAuthorizationException, DSException
  {
    DSHandle[] handles = { object.getHandle() };
    String[] link_types = { DSLinkDesc.containment} ;
    boolean with_terminal_object = false;
    
    // L'object résultat
    DSAncestries ancestries = new DSAncestries ();
    
    DSLinkIterator link_list = dssession.getSourcesLinkPairs(handles, link_types, true, null, new DSSelectSet(new String[] { "title" }));
    
    // Mettre les éléments de la liste dans un tableau pour faciliter les parcours
    Map<DSHandle,DSAncestryNode> nodes = new HashMap<DSHandle,DSAncestryNode> ();
    
    // Construit un graphe bidirectionnel des objets
    while (link_list.hasNext())
    {
      DSLinkItem li = link_list.nextObject();
      DSObject parent_object = li.getLinkedObject();
      DSHandle parent_handle = parent_object.getHandle();
      DSAncestryNode parent_node;
      parent_node = nodes.get(parent_handle);
      if (parent_node==null) {
        parent_node = new DSAncestryNode(parent_object);
        nodes.put(parent_handle, parent_node);
      }
      DSHandle child_handle = li.getHandle();
      DSAncestryNode child_node = nodes.get(child_handle);
      if (child_node==null) {
        child_node = new DSAncestryNode(dssession.getObject(child_handle));
        nodes.put(child_handle, child_node);
      }
      parent_node.children.add(child_node);
    }
    
    // Racines des lignées: la root_collection si fournie, ou les racines trouvées
    List<DSAncestryNode> roots = new LinkedList<DSAncestryNode>();
    if (root_collection!=null) {
      DSAncestryNode root_node = nodes.get(root_collection.getHandle());
      if (root_node!=null) {
        roots.add(root_node);
      }
      else {
        // La collection racine voulue n'est pas dans les ancètres => lignée vide
        return ancestries;
      }
    }
    else {
      // Recherche des racines: les objects racines de la base DocuShare
      Iterator<Entry<DSHandle,DSAncestryNode>> i_node = nodes.entrySet().iterator();
      while (i_node.hasNext()) {
        DSAncestryNode node = i_node.next().getValue();
        DSObject obj = node.object;
        Object is_root = obj.get(DSObject.isRoot);
        if (is_root!= null && (Boolean)is_root == true) {
          roots.add(node);
        }
      }
    }
    
    int num_last_path = 0; // à incrémenter à chaque parcours
    for (Iterator<DSAncestryNode> i_root=roots.iterator(); i_root.hasNext();) {
      DSAncestryNode root_node = i_root.next();
      
      // Parcours du graphe
      PathInformation path = new PathInformation(root_node, num_last_path+1);
      if (!all_paths) {
        path.maxPaths = 1; // Un seul chemin
      }
      makePaths (path, root_node);
      num_last_path = path.numPath;
      
      Iterator<DSAncestryNode[]> i_path = path.allPaths.iterator();
      while (i_path.hasNext()) {
        // Récupérer les lignées.
        // NB: Les chamins trouvés dans le graphe comportent les noeuds départ et arrivée, on ne veut
        // pas le noeud arrivée.
        DSAncestryNode[] path_nodes = i_path.next();
        int nb_objects = path_nodes.length;
        if (!with_terminal_object) {
          nb_objects--;
        }
        DSObject objects[] = new DSObject[nb_objects];
        for (int i=0; i<nb_objects; i++) {
          objects[i] = path_nodes[i].object;
        }
        ancestries.add(objects);
      }
    }
    return ancestries;

  }
  
  static class PathInformation {
    int numPath = 0;
    int maxPaths = 0;
    Stack<DSAncestryNode> currentStack = new Stack<DSAncestryNode>();
    List<DSAncestryNode[]> allPaths = new LinkedList<DSAncestryNode[]>();
    public PathInformation (DSAncestryNode root_node, int num_path)
    {
      numPath = num_path;
      currentStack.push(root_node);
      root_node.numRun = num_path;
    }
    boolean hasEnough ()
    {
      return (maxPaths>0 && allPaths.size()>=maxPaths);
    }
  }
  
  private static void makePaths (PathInformation current_path, DSAncestryNode current_node)
  {
    Iterator<DSAncestryNode> i_child = current_node.children.iterator();
    boolean end_of_path = true;
    while (i_child.hasNext()) {
      DSAncestryNode child_node = i_child.next();
      if (child_node.numRun < current_path.numPath) {
        // pas déjà parcouru dans ce chemin
        child_node.numRun = current_path.numPath;
        current_path.currentStack.push(child_node);
        end_of_path = false;
        
        // Suite du parcours
        makePaths (current_path, child_node);
        
        if (current_path.hasEnough ()) return;
        
        // Enlever le noeud
        current_path.currentStack.pop();
        
        // Tentative de chemin suivant : incrémenter le numéro de parcours, tous les noeuds de la pile prennent ce numéro
        current_path.numPath++;
        for (Iterator<DSAncestryNode> i_node = current_path.currentStack.iterator(); i_node.hasNext(); ) {
          i_node.next().numRun = current_path.numPath;
        }
      }
    }
    if (end_of_path) {
      // On est arrivé à un bout du graphe 
      
      // Enregistrement du parcours
      
      DSAncestryNode[] found_path = new DSAncestryNode[current_path.currentStack.size()];
      current_path.currentStack.toArray(found_path);
      current_path.allPaths.add(found_path);
    }
  }
  
  public static DSObject lookupChildByProp (DSSession dssession, DSCollection parent, String prop, Object value) throws DSAuthorizationException, DSException {
    DSObject child = null;
    DSObjectIterator item = parent.children(new DSSelectSet(prop));
    while (item.hasNext()) {
      DSObject obj = item.nextObject();
      Object obj_prop = obj.get(prop);
      if (value.equals(obj_prop)) {
        child = obj;
        break;
      }

      // Les valeurs d'objets sont différentes mais si la valeur attendue est une chaine
      // on va tenter de convertir en String pour les attributs d'un autre type
      String str_obj_prop = "";
      if (obj_prop!=null) {
        str_obj_prop = obj_prop.toString();
      }
      if (value.equals(str_obj_prop)) {
        child = obj;
        break;
      }

    }
    return child;
  }
  public static DSObject lookupChildByProps (DSSession dssession, DSCollection parent, Map<String,Object> props) throws DSAuthorizationException, DSException {
    return lookupChildByProps (dssession, parent, props, false);
  }
  
  public static DSObject lookupChildByProps (DSSession dssession, DSCollection parent, Map<String,Object> props, boolean use_regex) throws DSAuthorizationException, DSException {
    DSObject child = null;
    String [] prop_names = props.keySet().toArray(new String[0]);
    DSObjectIterator item = parent.children(new DSSelectSet(prop_names));
    while (item.hasNext()) {
      DSObject obj = item.nextObject();
      boolean good_obj = true;
      for (String prop: prop_names) {
        Object wanted_prop = props.get(prop);
        // Cas particulier: classe
        if (classProp.equals(prop)) {
          if (!obj.isTypeOf((String) wanted_prop)) {
            good_obj = false;
            break;
          }
        }
        else {
          if (use_regex && wanted_prop instanceof String) {
            // Vérification avec regexp
            String obj_prop = obj.get(prop).toString();
            String wanted_string = (String) wanted_prop;
            if (!obj_prop.matches(wanted_string)) {
              good_obj = false;
              break;
            }
          }
          else {
            Object obj_prop = obj.get(prop);
            if (!wanted_prop.equals(obj_prop)) {
              // Les valeurs d'objets sont différentes mais si la valeur attendue est une chaine
              // on va tenter de convertir en String pour les attributs d'un autre type
              String str_obj_prop = "";
              if (obj_prop!=null) {
                str_obj_prop = obj_prop.toString();
              }
              if (!wanted_prop.equals(str_obj_prop)) {
                good_obj = false;
                break;
              }
            }
          }
        }
      }
      if (good_obj) {
        child = obj;
        break;
      }
    }
    return child;
  }
  
  public static DSObject lookupChildByTitle (DSSession dssession, DSCollection parent, String title) throws DSAuthorizationException, DSException {
    return lookupChildByProp(dssession, parent, DSObject.title, title);
  }
  
  public static void addProps (DSProperties objProps, Map<String,Object> props) {
    for (Entry<String,Object> e: props.entrySet()) {
      String prop_name = e.getKey();
      Object prop_value = e.getValue();
      // Si la valeur est une chaine on vérifie s'il faut convertir suivant
      // le type de propriété, sinon on considère que le type d'objet est
      // le bon (exception sinon).
      if (prop_value instanceof String) {
        String s_value = (String) prop_value;
        DSPropDesc prop = objProps.getPropDesc(prop_name);
        if (prop!=null) {
          switch (prop.getType()) {
          case DSPropTypes.INTEGER:
            prop_value = Integer.parseInt(s_value);
            break;
          case DSPropTypes.FLOAT:
            prop_value = Float.parseFloat(s_value);
            break;
          case DSPropTypes.BOOLEAN:
            prop_value = Boolean.parseBoolean(s_value);
            break;
          case DSPropTypes.LONG:
            prop_value = Long.parseLong(s_value);
            break;
          }
        }
      }
      objProps.setPropValue(prop_name, prop_value);
    }
  }

  public static DSDocument createDocument (DSSession dssession, DSCollection parent, String document_class, Map<String,Object> props, File content_file, boolean is_temp, String ds_filename) throws DSException {
    DSProperties doc_props = dssession.getDSClass(document_class).createPrototype();

    // Set the required properties
    addProps (doc_props, props);

    DSClass version_class = dssession.getDSClass(DSVersion.classname);
    DSProperties version_props = version_class.createPrototype();
    addProps (version_props, props);
    
    DSClass rendition_class = dssession.getDSClass(DSRendition.classname);
    DSProperties rendition_props = rendition_class.createPrototype();

    DSContentElement[] contents = new DSContentElement[] { new FileContentElement(content_file.getPath(), ds_filename, is_temp) };
    String content_type = null;
    DSLoginPrincipal owner = null;
    DSAclEntry[] acl = null;
    
    DSHandle doc_handle = dssession.createDocument(doc_props, version_props, rendition_props, contents, content_type, DSLinkDesc.containment, parent, owner, acl);
    DSDocument doc = (DSDocument) dssession.getObject(doc_handle);
    return doc;
  }
  
  public static DSVersion createVersion(DSSession dssession, DSDocument doc, Map<String,Object> props, File content_file, String ds_filename) throws DSException {
	    DSClass version_class = dssession.getDSClass(DSVersion.classname);
	    DSProperties version_props = version_class.createPrototype();
	    addProps (version_props, props);
	    
	    DSClass rendition_class = dssession.getDSClass(DSRendition.classname);
	    DSProperties rendition_props = rendition_class.createPrototype();

	    DSContentElement[] contents = new DSContentElement[] { new FileContentElement(content_file.getPath(), ds_filename, true) };
	    String content_type = null;
	    DSLoginPrincipal owner = null;
	    DSAclEntry[] acl = null;
	    
	    DSHandle version_handle = doc.addVersion(version_props, rendition_props, contents);
	    DSVersion version = (DSVersion) dssession.getObject(version_handle);
	    return version;
  }
  
  public static DSCollection createCollection (DSSession dssession, DSCollection parent, String collection_class, Map<String,Object> props) throws DSAuthorizationException, DSException {
    DSProperties colProp = dssession.getDSClass(collection_class).createPrototype();

    // Set the required properties
    addProps (colProp, props);

    // create the collection
    DSHandle newCollHnd = dssession.createObject(colProp, DSLinkDesc.containment, parent, null, null);

    DSCollection coll = (DSCollection) dssession.getObject(newCollHnd, DSSelectSet.NO_PROPERTIES);
    return coll;
  }
  
  // Vérification qu'un objet est au bon endroit dans un plan de classement, et déplacement
  // éventuel.
  public static boolean isAtRightLocation(DSObject obj, DSCollection expected_parent, DSHandle root_handle, boolean move) throws DSAuthorizationException, DSException {
    // La collection parente a-t-elle changé ?
    boolean dossier_dans_bon_parent = false;
    DSObjectIterator i_parent = obj.getParents(null);
    while (i_parent.hasNext()) {
      DSCollection coll_parent = (DSCollection) i_parent.nextObject();
      if (coll_parent.getHandle().equals(expected_parent.getHandle())) {
        dossier_dans_bon_parent = true;
        break;
      }
      
      else if (move) {
        // Le dossier parent n'est pas celui attendu.
        // Pour le déplacement, il faut enlever l'objet de sa collection parente
        // à condition que celle-ci soit dans l'arborescence sous root, les
        // éventuels liens direct sur l'objet hors plan de classement ne sont pas 
        // modifiés.
        logger.info("Déplacement de la branche "+obj.getHandle()+" depuis la collection"+coll_parent.getHandle()+" vers la collection "+expected_parent.getHandle());
        if (isAncestor(coll_parent, root_handle)) {
          // On a remonté à la racine du plan de classement via ce parent, on supprime le lien
          coll_parent.removeChild(obj);
        }
      }
    }
    if (!dossier_dans_bon_parent && move) {
      // Ajouter dans le bon dossier
      expected_parent.addChild(obj);
    }
    
    return dossier_dans_bon_parent;
  }
  
  public static boolean isAncestor (DSObject object, DSHandle root_handle) throws DSAuthorizationException, DSException {
    DSObjectIterator i_ancestor = object.getSources(DSLinkDesc.containment, true, null);
    while (i_ancestor.hasNext()) {
      DSCollection coll_ancestor = (DSCollection) i_ancestor.nextObject();
      if (coll_ancestor.getHandle().equals(root_handle)) {
        // On a remonté à la racine voulue via les liens de containment
        return true;
      }
    }
    return false;
  }

}
