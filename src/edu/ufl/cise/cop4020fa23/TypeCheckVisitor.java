package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Stack;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;
import java.util.Set;
import java.util.HashSet;
public class TypeCheckVisitor implements ASTVisitor {
    private SymbolTable symbolTable = new SymbolTable();
    private Set<String> declaredNames = new HashSet<>();
    private Stack<SymbolTable> symbolTableStack = new Stack<>();
    private Stack<Set<String>> declaredNamesStack = new Stack<>();
    private Stack<Set<String>> scopeStack = new Stack<>();
    Program root;
    public TypeCheckVisitor() {
        declaredNamesStack = new Stack<>();
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
    	System.out.println("Visiting Assignment Statement. Variable: " + assignmentStatement.getlValue().getName());
        LValue lValue = assignmentStatement.getlValue();
        lValue.visit(this, arg);  
        
        Type lValueType = lValue.getVarType();
        if (lValueType == null) {
            throw new TypeCheckException("Undefined identifier in LValue: " + lValue.getName());
        }

        Type exprType = (Type) assignmentStatement.getE().visit(this, arg); 
        System.out.println("Expression type: " + exprType);


        return null;
    }



    
    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        Type leftType = (Type) binaryExpr.getLeftExpr().visit(this, arg);
        Type rightType = (Type) binaryExpr.getRightExpr().visit(this, arg);
        Kind opKind = binaryExpr.getOpKind();

        if (leftType == null || rightType == null) {
            throw new TypeCheckException("Undefined identifier in binary expression");
        }

        switch (opKind) {
            case PLUS:
                if (leftType == Type.STRING && rightType == Type.STRING) {
                    binaryExpr.setType(Type.STRING);
                    return Type.STRING;
                } else if (leftType == Type.INT && rightType == Type.INT) {
                    binaryExpr.setType(Type.INT);
                    return Type.INT;
                } else if (leftType == Type.PIXEL && rightType == Type.PIXEL) {
                    binaryExpr.setType(Type.PIXEL);
                    return Type.PIXEL;
                } else {
                    throw new TypeCheckException("Type mismatch for 'PLUS' operation");
                }

            case MINUS:
            case TIMES:
                if (leftType != Type.INT || rightType != Type.INT) {
                    throw new TypeCheckException("Arithmetic operations require INT operands");
                }
                binaryExpr.setType(Type.INT);
                return Type.INT;

            case DIV:
                if (leftType == Type.IMAGE && rightType == Type.INT) {
                    binaryExpr.setType(Type.IMAGE);
                    return Type.IMAGE;
                } else if (leftType == Type.INT && rightType == Type.INT) {
                    binaryExpr.setType(Type.INT);
                    return Type.INT;
                } else {
                    throw new TypeCheckException("Invalid operands for DIV operation");
                }

            case MOD:
                if (leftType != Type.INT || rightType != Type.INT) {
                    throw new TypeCheckException("MOD operation requires INT operands");
                }
                binaryExpr.setType(Type.INT);
                return Type.INT;

            case EQ:
            case LT:
            case LE:
            case GT:
            case GE:
                if (leftType != rightType) {
                    throw new TypeCheckException("Comparison operation type mismatch");
                }
                binaryExpr.setType(Type.BOOLEAN);
                return Type.BOOLEAN;

            case AND:
            case OR:
                if (leftType != Type.BOOLEAN || rightType != Type.BOOLEAN) {
                    throw new TypeCheckException("Logical operation type mismatch");
                }
                binaryExpr.setType(Type.BOOLEAN);
                return Type.BOOLEAN;

            case EXP:
                if (leftType != Type.INT || rightType != Type.INT) {
                    throw new TypeCheckException("EXP operation requires INT operands");
                }
                binaryExpr.setType(Type.INT);
                return Type.INT;

            default:
                throw new TypeCheckException("Unsupported binary operation: " + opKind);
        }
    }

    
    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        System.out.println("Entering Block - Pushing new scope");
        symbolTable.enterScope();
        System.out.println("Entering scope: " + symbolTable.getCurrentScope());

        for (Block.BlockElem elem : block.getElems()) {
            elem.visit(this, arg);
        }

        symbolTable.leaveScope();
        System.out.println("Exiting Block - Popping scope");
        return block;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        booleanLitExpr.setType(Type.BOOLEAN);
        return Type.BOOLEAN;
    }



    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        boolean isLValueContext = (arg instanceof LValue);

        Kind colorKind = channelSelector.color();

        if (colorKind != Kind.RES_red && colorKind != Kind.RES_green && colorKind != Kind.RES_blue) {
            throw new PLCCompilerException("Invalid channel selector color");
        }

        if (isLValueContext) {
            LValue lValue = (LValue) arg;
            Type lValueType = lValue.getVarType();

            if (lValueType != Type.PIXEL) {
                throw new PLCCompilerException("Channel selector can only be used with PIXEL type");
            }
        }

        return colorKind;
    }


    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        Type guardType = (Type) conditionalExpr.getGuardExpr().visit(this, arg);

        if (guardType != Type.BOOLEAN) {
            throw new PLCCompilerException("Conditional guard expression must have BOOLEAN type");
        }

        Type trueType = (Type) conditionalExpr.getTrueExpr().visit(this, arg);
        Type falseType = (Type) conditionalExpr.getFalseExpr().visit(this, arg);

        if (trueType != falseType) {
            throw new PLCCompilerException("Conditional expressions must have the same type");
        }

        conditionalExpr.setType(trueType);
        return trueType;
    }


    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        String constName = constExpr.getName();

        Type constType;
        switch (constName) {
            case "Z":
                constType = Type.INT;
                break;
            case "BLACK":
            case "BLUE":
            case "CYAN":
            case "DARK_GRAY":
            case "GRAY":
            case "GREEN":
            case "LIGHT_GRAY":
            case "MAGENTA":
            case "ORANGE":
            case "PINK":
            case "RED":
            case "WHITE":
            case "YELLOW":
                constType = Type.PIXEL;
                break;
            case "TRUE":
            case "FALSE":
                constType = Type.BOOLEAN;
                break;
            default:
                throw new PLCCompilerException("Undefined constant: " + constName);
        }

        constExpr.setType(constType); 
        return constType; 
    }


    
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        NameDef nameDef = declaration.getNameDef();
        String varName = nameDef.getName();
        Type declaredType = nameDef.getType();

        System.out.println("Visiting Declaration: Variable " + varName + " of Type " + declaredType);

        Expr initializer = declaration.getInitializer();
        if (initializer != null) {
            Type initType = (Type) initializer.visit(this, arg);
            if (declaredType != initType && !(declaredType == Type.IMAGE && initType == Type.STRING)) {
                throw new TypeCheckException("Type of expression and declared type do not match in declaration of " + varName);
            }
        }

        boolean isDeclaredLocally = symbolTable.lookupLocal(varName);
        if (!isDeclaredLocally) {
            symbolTable.insert(nameDef);
            System.out.println("Declaration successfully inserted into symbol table: " + varName);
        } else {
            System.out.println("Duplicate declaration in the same scope: " + varName);
            throw new TypeCheckException("Duplicate declaration of variable: " + varName);
        }

        return null;
    }



    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        System.out.println("Visiting Dimension: " + dimension);

        Expr widthExpr = dimension.getWidth();
        System.out.println("Visiting width expression: " + widthExpr);
        Type widthType = (Type) widthExpr.visit(this, arg);
        if (widthType != Type.INT) {
            throw new TypeCheckException("Width of the dimension must be of type INT");
        }

        Expr heightExpr = dimension.getHeight();
        System.out.println("Visiting height expression: " + heightExpr);
        Type heightType = (Type) heightExpr.visit(this, arg);
        if (heightType != Type.INT) {
            throw new TypeCheckException("Height of the dimension must be of type INT");
        }

        System.out.println("Dimension types: Width - " + widthType + ", Height - " + heightType);
        return dimension;
    }


    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        System.out.println("Entering Do Statement");

        symbolTable.enterScope(); 

        for (GuardedBlock guardedBlock : doStatement.getGuardedBlocks()) {
            guardedBlock.visit(this, arg);
        }

        symbolTable.leaveScope(); 
        System.out.println("Exiting Do Statement");
        return null;
    }





    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        Type redType = (Type) expandedPixelExpr.getRed().visit(this, arg);
        Type greenType = (Type) expandedPixelExpr.getGreen().visit(this, arg);
        Type blueType = (Type) expandedPixelExpr.getBlue().visit(this, arg);

        if ((redType != Type.INT && redType != Type.PIXEL) ||
            (greenType != Type.INT && greenType != Type.PIXEL) ||
            (blueType != Type.INT && blueType != Type.PIXEL)) {
            throw new TypeCheckException("ExpandedPixelExpr components must have type INT or PIXEL");
        }

        return Type.PIXEL;
    }


    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        Type guardType = (Type) guardedBlock.getGuard().visit(this, arg);

        if (guardType != Type.BOOLEAN) {
            throw new PLCCompilerException("Guard expression must have type BOOLEAN");
        }

        guardedBlock.getBlock().visit(this, arg);

        return Type.VOID;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        String identifierName = identExpr.getName();
        System.out.println("Visiting identifier expression: " + identifierName);

        NameDef nameDef = symbolTable.lookup(identifierName);
        if (nameDef != null) {
            identExpr.setNameDef(nameDef);
            identExpr.setType(nameDef.getType());
            System.out.println("Identifier found in scope: " + identifierName + ", Type: " + nameDef.getType());
            return nameDef.getType();
        }

        System.out.println("Identifier not found in any visible scope: " + identifierName);
        throw new TypeCheckException("Undefined identifier: " + identifierName);
    }




    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        System.out.println("Visiting IfStatement");

        if (ifStatement.getGuardedBlocks().isEmpty()) {
            throw new PLCCompilerException("If statement must have at least one guarded block");
        }

        for (GuardedBlock guardedBlock : ifStatement.getGuardedBlocks()) {
            System.out.println("Visiting guard expression");
            Type guardType = (Type) guardedBlock.getGuard().visit(this, arg);
            if (guardType != Type.BOOLEAN) {
                throw new PLCCompilerException("Guard expression must have BOOLEAN type");
            }

            System.out.println("Visiting block inside GuardedBlock");
            for (Block.BlockElem elem : guardedBlock.getBlock().getElems()) {
                elem.visit(this, arg);
            }
        }

        return null;
    }


    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        System.out.println("Visiting LValue: " + lValue.getName());

        if (lValue.getNameDef() == null) {
            NameDef nameDef = symbolTable.lookup(lValue.getName());
            if (nameDef == null) {
                throw new TypeCheckException("Undefined LValue: " + lValue.getName());
            }
            lValue.setNameDef(nameDef);  
        }

        if (lValue.getPixelSelector() != null) {
            lValue.getPixelSelector().visit(this, arg);
            if (lValue.getVarType() != Type.IMAGE) {
                throw new TypeCheckException("Pixel selector can only be used with IMAGE type");
            }
        }

        if (lValue.getChannelSelector() != null) {
            lValue.getChannelSelector().visit(this, arg);
            if (lValue.getVarType() != Type.PIXEL && lValue.getVarType() != Type.IMAGE) {
                throw new TypeCheckException("Channel selector can only be used with PIXEL or IMAGE type");
            }
        }

        return lValue.getVarType();
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        Type declaredType = nameDef.getType();

        if (declaredType != Type.INT && declaredType != Type.BOOLEAN && declaredType != Type.STRING && declaredType != Type.IMAGE && declaredType != Type.PIXEL) {
            throw new PLCCompilerException("Invalid type for name definition: " + declaredType);
        }

        Dimension dimension = nameDef.getDimension();
        if (dimension != null) {
            dimension.visit(this, arg);

            Type widthType = dimension.getWidth().getType();
            Type heightType = dimension.getHeight().getType();

            if (widthType != Type.INT || heightType != Type.INT) {
                throw new PLCCompilerException("Dimension width and height must be of type INT");
            }
        }

        return null;
    }


    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        System.out.println("Visiting NumLitExpr: " + numLitExpr.getText());
        numLitExpr.setType(Type.INT);  
        return Type.INT;
    }





    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        System.out.println("Visiting Pixel Selector in Expression");

        Expr xExpr = pixelSelector.xExpr();
        Type xExprType = (Type) xExpr.visit(this, arg);
        if (xExpr instanceof IdentExpr && xExprType == null) {
            throw new TypeCheckException("Undefined identifier in PixelSelector for X coordinate: " + ((IdentExpr) xExpr).getName());
        }

        Expr yExpr = pixelSelector.yExpr();
        Type yExprType = (Type) yExpr.visit(this, arg);
        if (yExpr instanceof IdentExpr && yExprType == null) {
            throw new TypeCheckException("Undefined identifier in PixelSelector for Y coordinate: " + ((IdentExpr) yExpr).getName());
        }

        return null;
    }





    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        Expr primaryExpr = postfixExpr.primary();
        Type primaryType = (Type) primaryExpr.visit(this, arg);
        if (primaryType == null) {
            throw new TypeCheckException("Primary expression in postfix expression does not have a valid type");
        }

        PixelSelector pixelSelector = postfixExpr.pixel();
        if (pixelSelector != null) {
            pixelSelector.visit(this, arg);
            if (primaryType != Type.IMAGE) {
                throw new TypeCheckException("Pixel selector can only be used with IMAGE type");
            }
        }

        ChannelSelector channelSelector = postfixExpr.channel();
        if (channelSelector != null) {
            channelSelector.visit(this, arg);
            if (primaryType != Type.PIXEL && (pixelSelector == null || primaryType != Type.IMAGE)) {
                throw new TypeCheckException("Channel selector can only be used with PIXEL or IMAGE type");
            }
            postfixExpr.setType(Type.INT); 
            return Type.INT;
        }

        postfixExpr.setType(primaryType);
        return primaryType;
    }


    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        System.out.println("Entering Program: " + program.getName());
        root = program;
        Type type = Type.kind2type(program.getTypeToken().kind());
        program.setType(type);

        symbolTableStack.push(symbolTable);

        symbolTable.enterScope();
        System.out.println("Entering scope for program parameters: " + symbolTable.getCurrentScope());
        for (NameDef param : program.getParams()) {
            if (!symbolTable.insert(param)) {
                throw new TypeCheckException("Duplicate parameter name: " + param.getName());
            }
        }

        symbolTable.enterScope();
        System.out.println("Entering scope for local variables: " + symbolTable.getCurrentScope());

        program.getBlock().visit(this, arg);

        symbolTable.leaveScope();
        System.out.println("Exiting scope for local variables");

        symbolTable.leaveScope();
        System.out.println("Exiting scope for program parameters");

        symbolTableStack.pop();
        return type;
    }


    
    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        System.out.println("Visiting ReturnStatement: " + returnStatement);

        if (returnStatement.getE() != null) {
            System.out.println("Return statement has expression: " + returnStatement.getE());
            Type exprType = (Type) returnStatement.getE().visit(this, arg);

            Kind functionKind = root.getTypeToken().kind();
            Type expectedReturnType = Type.kind2type(functionKind);
            if (exprType != expectedReturnType) {
                throw new TypeCheckException("Return type mismatch. Expected: " + expectedReturnType + ", Found: " + exprType);
            }
        }
        return null;
    }




    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        symbolTable.enterScope();

        for (Block.BlockElem elem : statementBlock.getBlock().getElems()) {
            elem.visit(this, arg);
        }

        symbolTable.leaveScope();

        return null;
    }


    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        System.out.println("Visiting StringLitExpr: " + stringLitExpr);
        stringLitExpr.setType(Type.STRING); 
        System.out.println("Setting type of StringLitExpr to STRING");
        return Type.STRING; 
    }




    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        Type exprType = (Type) unaryExpr.getExpr().visit(this, arg);

        switch (unaryExpr.getOp()) {
            case MINUS:
                if (exprType != Type.INT) {
                    throw new TypeCheckException("Unary minus (-) can only be applied to INT expressions.");
                }
                unaryExpr.setType(Type.INT);
                return Type.INT;

            case BANG:
                if (exprType != Type.BOOLEAN) {
                    throw new TypeCheckException("Logical NOT (!) can only be applied to BOOLEAN expressions.");
                }
                unaryExpr.setType(Type.BOOLEAN);
                return Type.BOOLEAN;

            case RES_width:
            case RES_height:
                if (exprType != Type.IMAGE) {
                    throw new TypeCheckException("RES_width and RES_height can only be applied to IMAGE expressions.");
                }
                return Type.INT; 

            default:
                throw new PLCCompilerException("Unsupported unary operation: " + unaryExpr.getOp());
        }
    }
    
    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        Type exprType = (Type) writeStatement.getExpr().visit(this, null);
        if (exprType != Type.INT && exprType != Type.STRING && exprType != Type.BOOLEAN && exprType != Type.PIXEL) {
            throw new PLCCompilerException("Invalid type for write statement: " + exprType);
        }
        return null;
    }
}
