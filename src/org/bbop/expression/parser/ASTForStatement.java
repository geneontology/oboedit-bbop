/* Generated By:JJTree: Do not edit this line. ASTForStatement.java */

package org.bbop.expression.parser;

import org.bbop.expression.JexlContext;
import org.bbop.expression.util.Coercion;

import org.apache.log4j.*;

public class ASTForStatement extends SimpleNode {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(ASTForStatement.class);
  public ASTForStatement(int id) {
    super(id);
  }

  public ASTForStatement(Parser p, int id) {
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
      Object result = null;
      /* initialize the loop variable */
      ASTAssignment loopVariable = (ASTAssignment) jjtGetChild(0);
      loopVariable.value(jc);
      
      /* second child is the condition to evaluate */
      SimpleNode condition = (SimpleNode) jjtGetChild(1);
      while (Coercion.coerceBoolean(condition.value(jc)).booleanValue()) {
          // execute statement
      	try {
      		result = ((SimpleNode) jjtGetChild(3)).value(jc);
      	} catch (BreakLoopException ex) {
      		return null;
      	}
      	((SimpleNode) jjtGetChild(2)).value(jc);
      }
      return result;
  }
}