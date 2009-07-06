/*
 * Copyright 2002-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bbop.expression.parser;

import org.bbop.expression.JexlContext;

/**
 * represents equality between integers - use .equals() for strings.
 * 
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id: ASTExpressionExpression.java,v 1.2 2007/09/27 01:02:09 jmr39 Exp $
 */
import org.apache.log4j.*;

public class ASTExpressionExpression extends SimpleNode {

	//initialize logger
	protected final static Logger logger = Logger.getLogger(ASTExpressionExpression.class);

    /**
     * Create the node given an id.
     * 
     * @param id node id.
     */
    public ASTExpressionExpression(int id) {
        super(id);
    }

    /**
     * Create a node with the given parser and id.
     * 
     * @param p a parser.
     * @param id node id.
     */
    public ASTExpressionExpression(Parser p, int id) {
        super(p, id);
    }

    /** {@inheritDoc} */
    @Override
	public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /** {@inheritDoc} */
    @Override
	public Object value(JexlContext context) throws Exception {
        return ((SimpleNode) jjtGetChild(0)).value(context);
    }
}
