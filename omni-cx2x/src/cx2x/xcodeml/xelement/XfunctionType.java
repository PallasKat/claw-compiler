/*
 * This file is released under terms of BSD license
 * See LICENSE file for more information
 */

package cx2x.xcodeml.xelement;

import cx2x.xcodeml.xnode.Xattr;
import cx2x.xcodeml.xnode.Xcode;
import cx2x.xcodeml.xnode.Xnode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The XfunctionType represents the FfunctionType (3.4) element in XcodeML
 * intermediate representation.
 *
 * Elements: (params?)
 * - Optional:
 *   - params (Xparams)
 * Attributes:
 * - Required: type (text), return_type (text)
 * - Optional: result_name (text), is_recursive (bool), is_program (bool),
 *   is_internal (bool)
 *
 * @author clementval
 */

public class XfunctionType extends Xtype {

  // Elements
  private Xparams _params = null;

  private String _returnType = null;

  // optional attributes
  private String _resultName = null;
  private boolean _isRecursive = false;
  private boolean _isProgram = false;
  private boolean _isInternal = false;

  /**
   * Xelement standard ctor. Pass the base element to the base class and read
   * inner information (elements and attributes).
   * @param baseElement The root element of the Xelement
   */
  public XfunctionType(Element baseElement){
    super(baseElement);
    readElementInformation();
  }

  /**
   * Read inner element information.
   */
  private void readElementInformation(){
    Xnode paramsNode = find(Xcode.PARAMS);
    _params = new Xparams(paramsNode.getElement());
    _returnType = getAttribute(Xattr.RETURN_TYPE);
    _isProgram = getBooleanAttribute(Xattr.IS_PROGRAM);

    // read optional attributes
    _resultName = getAttribute(Xattr.RESULT_NAME);
    _isRecursive = getBooleanAttribute(Xattr.IS_RECURSIVE);
    _isProgram = getBooleanAttribute(Xattr.IS_PROGRAM);
    _isInternal = getBooleanAttribute(Xattr.IS_INTERNAL);
  }

  /**
   * Get the function result name.
   * @return Result name value.
   */
  public String getResultName(){
    return _resultName;
  }

  /**
   * Check whether function is recursive.
   * @return True if the function is recursive. False otherwise.
   */
  public boolean isRecursive(){
    return _isRecursive;
  }

  /**
   * Check whether function is internal.
   * @return True if the function is internal. False otherwise.
   */
  public boolean isInternal(){
    return _isInternal;
  }

  /**
   * Get the function return type.
   * @return The function's return type as String.
   */
  public String getReturnType(){
    return _returnType;
  }

  /**
   * Check whether function is the program function.
   * @return True if the function is the program function. False otherwise.
   */
  public boolean isProgram(){
    return _isProgram;
  }


  /**
   * Get the params element.
   * @return Params element.
   */
  public Xparams getParams(){
    return _params;
  }

  /**
   * A new object XfunctionType that is the clone of the current object.
   * @return A new XfunctionType that is a clone of the current one.
   */
  public XfunctionType cloneObject() {
    Node clone = cloneNode();
    return new XfunctionType((Element) clone);
  }

}
