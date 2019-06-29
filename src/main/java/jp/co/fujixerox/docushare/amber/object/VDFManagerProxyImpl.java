package jp.co.fujixerox.docushare.amber.object;

import com.xerox.docushare.DSClass;
import com.xerox.docushare.DSException;
import java.util.Map;
import jp.co.fujixerox.docushare.amber.DSWebException;
import jp.co.fujixerox.docushare.amber.VDFInvalidArgumentException;
import jp.co.fujixerox.docushare.amber.VDFNoSuchMethodException;
import jp.co.fujixerox.docushare.amber.VDFNotFoundException;
import jp.co.fujixerox.docushare.amber.VDFSession;
import jp.co.fujixerox.docushare.amber.vdf.DefineElement;
import jp.co.fujixerox.docushare.amber.vdf.VDF;
import jp.co.fujixerox.docushare.amber.vdf.VDFClosure;
import jp.co.fujixerox.docushare.amber.vdf.VDFContext;

public class VDFManagerProxyImpl extends VDFBaseProxyObject
  implements VDFManagerProxy
{
  private VDFRequest request;
  private VDFExtensionImpl extension = null;

  VDFManagerProxyImpl(VDFSession session, VDFRequest request)
  {
    super(session);
    this.request = request;
  }

  public VDFClosure lookup(@SuppressWarnings("rawtypes") Map argmap)
    throws VDFInvalidArgumentException, VDFNotFoundException, DSWebException, DSException
  {
    String command = (String)argmap.get("command");
    String defineId = (String)argmap.get("id");
    String vdfName = null;
    VDF vdf = null;
    if (command == null) {
      vdfName = (String)argmap.get("vdfName");
      if (vdfName == null)
        throw new VDFInvalidArgumentException("command", null);
      vdf = this.session.lookupVDF(vdfName);
      if (vdf == null)
        throw new VDFNotFoundException(vdfName);
    } else {
      String classname = (String)argmap.get("type");
      vdfName = command + classname;

      vdf = this.session.lookupVDF(vdfName);
      if (vdf == null) {
        DSClass dsclass = this.session.getDSSession().getDSClass(classname);
        if (dsclass == null)
          throw new VDFInvalidArgumentException("type", classname);
        String originalClassName = dsclass.getOriginalClassName();
        vdfName = command + originalClassName;
        vdf = this.session.lookupVDF(vdfName);
        if (vdf == null) {
          vdfName = command + "Generic";
          vdf = this.session.lookupVDF(vdfName);
          if (vdf == null)
            throw new VDFNotFoundException(vdfName);
        }
      }
    }
    VDFContext context = new VDFContext(vdf, this.session, this.request);
    if (defineId == null) {
      return new VDFClosure(vdf, context, null);
    }
    DefineElement defineElement = context.lookupDefinition(defineId);
    if (defineElement == null)
      throw new VDFNoSuchMethodException(vdfName + ":" + defineId);
    if (!defineElement.hasPublicAccess())
    {
      throw new VDFNoSuchMethodException(vdfName + ":" + defineId + " " + " access='public'");
    }
    return new VDFClosure(defineElement, context, null);
  }

  public String toExternalForm()
  {
    return "[VDF Manager]";
  }
  
  public VDFExtensionImpl extension() throws DSWebException, DSException 
  {
    if (this.extension == null) {
      this.extension = VDFExtensionImpl.loadVDFExtension(this.session, this.request);
    }
    
    return this.extension;
  }

  static 
  {
  	System.out.println ("VDFManagerProxyImpl INIT"); 	
  }

}