/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.compiler.ir;

import org.eclipse.golo.runtime.OperatorType;

public final class Decorator extends GoloElement<Decorator> {

  private ExpressionStatement<?> expressionStatement;

  private boolean constant = false;

  Decorator(ExpressionStatement<?> expressionStatement) {
    super();
    setExpressionStatement(expressionStatement);
  }

  protected Decorator self() { return this; }

  public ExpressionStatement<?> getExpressionStatement() {
    return expressionStatement;
  }

  private boolean isValidDecoratorExpressoin(ExpressionStatement<?> expr) {
    return expr instanceof ReferenceLookup
          || expr instanceof FunctionInvocation
          || expr instanceof ClosureReference
          || (expr instanceof BinaryOperation
            && OperatorType.ANON_CALL.equals(((BinaryOperation) expr).getType()));
  }

  public void setExpressionStatement(ExpressionStatement<?> expr) {
    if (!isValidDecoratorExpressoin(expr)) {
      throw new IllegalArgumentException("Decorator expression must be a reference or an invocation, got a "
          + expr.getClass().getSimpleName());
    }
    this.expressionStatement = makeParentOf(expr);
  }

  public boolean isConstant() {
    return constant;
  }

  public void setConstant(boolean constant) {
    this.constant = constant;
  }

  public Decorator constant(boolean constant) {
    setConstant(constant);
    return this;
  }

  public Decorator constant() {
    return constant(true);
  }

  private ExpressionStatement<?> wrapLookup(ReferenceLookup reference, ExpressionStatement<?> expression) {
    return Builders.call(reference.getName())
      .constant(this.isConstant())
      .withArgs(expression);
  }

  private ExpressionStatement<?> wrapInvocation(FunctionInvocation invocation, ExpressionStatement<?> expression) {
    return Builders.anonCall(invocation, Builders.functionInvocation()
        .constant(this.isConstant())
        .withArgs(expression));
  }
  private ExpressionStatement<?> wrapAnonymousCall(ExpressionStatement<?> call, ExpressionStatement<?> expression) {
    return Builders.anonCall(call, Builders.functionInvocation().constant(this.isConstant()).withArgs(expression));
  }

  public ExpressionStatement<?> wrapExpression(ExpressionStatement<?> expression) {
    if (expressionStatement instanceof ReferenceLookup) {
      return wrapLookup((ReferenceLookup) expressionStatement, expression);
    }
    if (expressionStatement instanceof FunctionInvocation) {
      return wrapInvocation((FunctionInvocation) expressionStatement, expression);
    }
    return wrapAnonymousCall(ExpressionStatement.of(expressionStatement), expression);
  }

  @Override
  public void accept(GoloIrVisitor visitor) {
    visitor.visitDecorator(this);
  }

  @Override
  public void walk(GoloIrVisitor visitor) {
    expressionStatement.accept(visitor);
  }

  @Override
  protected void replaceElement(GoloElement<?> original, GoloElement<?> newElement) {
    if (expressionStatement.equals(original) && newElement instanceof ExpressionStatement) {
      setExpressionStatement(ExpressionStatement.of(newElement));
    } else {
      throw cantReplace(original, newElement);
    }
  }
}
