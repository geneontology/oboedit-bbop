/* Generated By:JJTree: Do not edit this line. ASTExportStatement.java */

package org.bbop.expression.parser;

import org.bbop.expression.ExpressionException;
import org.bbop.expression.JexlContext;
import org.apache.log4j.*;

public class ASTExportStatement extends SimpleNode {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(ASTExportStatement.class);
  public ASTExportStatement(int id) {
    super(id);
  }

  public ASTExportStatement(Parser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  @Override
public Object jjtAccept(ParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
  
  /** {@inheritDoc} */
  @Override
public Object value(JexlContext jc) throws Exception {
      ASTIdentifier v = (ASTIdentifier) jjtGetChild(0);
      try {
    	  jc.exportLocalVariable(v.val);
      } catch (ExpressionException ex) {
    	  ex.decorateException(this);
      }
      return null;
  }
}
