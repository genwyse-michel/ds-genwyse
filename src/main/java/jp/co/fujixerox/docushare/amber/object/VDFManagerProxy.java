package jp.co.fujixerox.docushare.amber.object;

import com.xerox.docushare.DSException;
import java.util.Map;
import jp.co.fujixerox.docushare.amber.DSWebException;
import jp.co.fujixerox.docushare.amber.VDFInvalidArgumentException;
import jp.co.fujixerox.docushare.amber.VDFNotFoundException;
import jp.co.fujixerox.docushare.amber.vdf.VDFClosure;

public abstract interface VDFManagerProxy extends VDFProxyObject
{
  @SuppressWarnings("rawtypes")
  public abstract VDFClosure lookup(Map paramMap)
    throws VDFInvalidArgumentException, VDFNotFoundException, DSWebException, DSException;

  public abstract VDFExtensionImpl extension()
    throws DSWebException, DSException;
  
}
