package jp.co.fujixerox.docushare.amber.object;

import com.xerox.docushare.DSException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import jp.co.fujixerox.docushare.amber.DSWebException;
import jp.co.fujixerox.docushare.amber.DSWebSystemException;
import jp.co.fujixerox.docushare.amber.VDFNoSuchMethodException;
import org.jdom.Document;
import org.jdom.Element;

public abstract class VDFProxyObjectImpl
  implements VDFProxyObject
{
  @SuppressWarnings("rawtypes")
  public static final Class[] classMapArgs = { Map.class };
  @SuppressWarnings("rawtypes")
  public static final Class[] classNoArgs = new Class[0];

  public Object invoke(String name, @SuppressWarnings("rawtypes") Map argmap)
    throws DSWebException, DSException
  {
    normalizeArgumentTypes(name, argmap);

    return invoke(name, classMapArgs, new Object[] { argmap });
  }

  public Object invoke(String name)
    throws DSWebException, DSException
  {
    return invoke(name, classNoArgs, null);
  }

  protected Object invoke(String name, @SuppressWarnings("rawtypes") Class[] parameterTypes, Object[] parameters)
    throws DSWebException, DSException
  {
    Method method = getPublishedMethod(name, parameterTypes);
    try {
      Object result = method.invoke(this, parameters);
      if ((result instanceof Document))
        return new VDFXMLDocumentProxyObjectImpl((Document)result);
      if ((result instanceof Element)) {
        return new VDFXMLElementProxyObjectImpl((Element)result);
      }
      return result;
    } catch (InvocationTargetException e) {
      Throwable ex = e.getTargetException();
      if ((ex instanceof DSWebException))
        throw ((DSWebException)ex);
      if ((ex instanceof DSException))
        throw ((DSException)ex);
      throw new DSWebSystemException(ex.getMessage(), ex); } catch (IllegalAccessException e) {
        throw new DSWebSystemException("VDFProxyObjectImpl: Impossible IllegalAccessException", e);
    }
  }

  @SuppressWarnings("rawtypes")
  protected Method getPublishedMethod(String name, Class[] parameterTypes)
    throws DSWebException
  {
    Class javaClass = getClass();
    while (!javaClass.equals(VDFProxyObjectImpl.class))
    {
      Method method = getPublishedMethodInSpecifiedClass(name, parameterTypes, javaClass);
      if (method != null)
        return method;
      javaClass = javaClass.getSuperclass();
    }

    throw new VDFNoSuchMethodException(name);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Method getPublishedMethodInSpecifiedClass(String name, Class[] parameterTypes, Class javaClass)
  {
    Class[] interfaces = javaClass.getInterfaces();
    for (int i = 0; i < interfaces.length; i++)
      try {
        return interfaces[i].getMethod(name, parameterTypes);
      }
      catch (NoSuchMethodException e)
      {
      }
    return null;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void normalizeArgumentTypes(String methodName, Map map) throws DSWebException {
    Iterator iterator = map.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry entry = (Map.Entry)(Map.Entry)iterator.next();
      String key = (String)(String)entry.getKey();
      Object value = entry.getValue();
      Object newValue = normalizeArgumentDispatch(methodName, key, value);
      if (value != newValue)
        map.put(key, newValue);
    }
  }

  protected Object normalizeArgumentDispatch(String methodName, String key, Object value)
    throws DSWebException
  {
    if ((value instanceof String))
      return value;
    if ((value instanceof VDFXMLProxyObject))
      return normalizeArgument(methodName, key, (VDFXMLProxyObject)value);
    if ((value instanceof VDFProxyObject))
      return normalizeArgument(methodName, key, (VDFProxyObject)value);
    if ((value instanceof Integer)) {
      return normalizeArgument(methodName, key, (Integer)value);
    }
    return normalizeArgument(methodName, key, value);
  }

  protected Object normalizeArgument(String methodName, String parameterName, Object value)
    throws DSWebException
  {
    return value.toString();
  }

  protected Object normalizeArgument(String methodName, String parameterName, VDFXMLProxyObject value)
    throws DSWebException
  {
    return value.getJDOMObject();
  }

  protected Object normalizeArgument(String methodName, String parameterName, VDFProxyObject value)
    throws DSWebException
  {
    return value.toExternalForm();
  }

  protected Object normalizeArgument(String methodName, String parameterName, Integer value)
    throws DSWebException
  {
    return value.toString();
  }
}