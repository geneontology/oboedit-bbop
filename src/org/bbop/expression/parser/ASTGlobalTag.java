/* Generated By:JJTree: Do not edit this line. ASTGlobalTag.java */

package org.bbop.expression.parser;

import org.apache.log4j.*;

public class ASTGlobalTag extends SimpleNode {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(ASTGlobalTag.class);
  public ASTGlobalTag(int id) {
    super(id);
  }

  public ASTGlobalTag(Parser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  @Override
public Object jjtAccept(ParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
