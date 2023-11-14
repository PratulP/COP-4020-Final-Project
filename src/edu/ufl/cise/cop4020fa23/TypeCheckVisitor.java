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
    Program root;
    public TypeCheckVisitor() {
        declaredNamesStack = new Stack<>();
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        LValue lValue = assignmentStatement.getlValue();
        Type lValueType = (Type) lValue.visit(this, arg); 
        System.out.println("LValue type: " + lValueType);

        Type exprType = (Type) assignmentStatement.getE().visit(this, arg); 
        System.out.println("Expression type: " + exprType);

        if (lValueType == null) {
            throw new TypeCheckException("Undefined identifier in LValue: " + lValue.getName());
        }

        if (lValueType == Type.IMAGE) {
            if (exprType != Type.PIXEL && exprType != Type.INT) {
                throw new TypeCheckException("Assignment type mismatch");
            }
        } else if (lValueType != exprType) {
            throw new TypeCheckException("Assignment type mismatch");
        }

        return null;
    }


    
    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        Type leftType = (Type) binaryExpr.getLeftExpr().visit(this, arg);
        Type rightType = (Type) binaryExpr.getRightExpr().visit(this, arg);
        Kind opKind = binaryExpr.getOpKind();

        if (leftType == null) {
            throw new TypeCheckException("Undefined identifier: " + binaryExpr.getLeftExpr());
        }

        if (rightType == null) {
            throw new TypeCheckException("Undefined identifier: " + binaryExpr.getRightExpr());
        }

        if (opKind == Kind.PLUS) {
            if (leftType == Type.STRING || rightType == Type.STRING) {
                binaryExpr.setType(Type.STRING); 
                return Type.STRING;
            }
            if (leftType == Type.INT && rightType == Type.INT) {
                binaryExpr.setType(Type.INT); 
                return Type.INT;
            }
            throw new TypeCheckException("Invalid types for + operation");
        } else if (opKind == Kind.MINUS || opKind == Kind.TIMES || opKind == Kind.DIV) {
            if (leftType != Type.INT || rightType != Type.INT) {
                throw new TypeCheckException("Arithmetic operation type mismatch");
            }
            binaryExpr.setType(Type.INT);
            return Type.INT;
        } else if (opKind == Kind.EQ || opKind == Kind.LT || opKind == Kind.LE || opKind == Kind.GT || opKind == Kind.GE) {
            if (leftType != rightType) {
                throw new TypeCheckException("Comparison operation type mismatch");
            }
            binaryExpr.setType(Type.BOOLEAN);
            return Type.BOOLEAN;
        } else if (opKind == Kind.AND || opKind == Kind.OR) {
            if (leftType != Type.BOOLEAN || rightType != Type.BOOLEAN) {
                throw new TypeCheckException("Logical operation type mismatch");
            }
            binaryExpr.setType(Type.BOOLEAN); 
            return Type.BOOLEAN;
        } else if (opKind == Kind.EXP) {
            if (leftType != Type.INT || rightType != Type.INT) {
                throw new TypeCheckException("Both operands of EXP operation must be of type INT");
            }
            binaryExpr.setType(Type.INT); 
            return Type.INT;
        } else {
            throw new TypeCheckException("Unsupported binary operation: " + opKind);
        }
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        System.out.println("Entering Block - Pushing new scope");
        symbolTable.enterScope();
        declaredNamesStack.push(new HashSet<>());

        for (Block.BlockElem elem : block.getElems()) {
            System.out.println("Visiting Block Element: " + elem.getClass().getSimpleName());
            elem.visit(this, arg);
        }

        declaredNamesStack.pop();
        symbolTable.leaveScope(); 
        System.out.println("Exiting Block - Popping scope");
        return block;
    }





    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        System.out.println("Visiting BooleanLitExpr: " + booleanLitExpr.getText());
        booleanLitExpr.setType(Type.BOOLEAN); 
        System.out.println("Setting type of BooleanLitExpr to BOOLEAN");
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


    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        NameDef nameDef = declaration.getNameDef();

        System.out.println("Visiting Declaration: " + nameDef.getName() + ", Type: " + nameDef.getType() + ", Scope Level: " + symbolTableStack.size());
        String name = nameDef.getName();

        if (declaredNamesStack.peek().contains(name)) {
            throw new TypeCheckException("Variable already declared in this scope: " + name);
        }

        declaredNamesStack.peek().add(name);
        symbolTableStack.peek().insert(nameDef);

        Expr initializer = declaration.getInitializer();
        Type declaredType = nameDef.getType();
        if (initializer != null) {
            Type initializedType = (Type) initializer.visit(this, arg);
            if (!(declaredType == Type.IMAGE && initializedType == Type.STRING) && declaredType != initializedType) {
                System.out.println("Type mismatch detected in declaration. Declared Type: " + declaredType + ", Initializer Type: " + initializedType);
                throw new TypeCheckException("Type mismatch in declaration");
            }
        }

        if (nameDef.getDimension() != null) {
            nameDef.getDimension().visit(this, arg);
        }

        return null;
    }



    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        System.out.println("Visiting Dimension: " + dimension);

        System.out.println("Visiting width expression: " + dimension.getWidth());
        Type widthType = (Type) dimension.getWidth().visit(this, arg);
        System.out.println("Width type: " + widthType);

        System.out.println("Visiting height expression: " + dimension.getHeight());
        Type heightType = (Type) dimension.getHeight().visit(this, arg);
        System.out.println("Height type: " + heightType);

        if (widthType != Type.INT || heightType != Type.INT) {
            throw new PLCCompilerException("Dimension width and height must be of type INT");
        }
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

        if (redType != Type.PIXEL || greenType != Type.PIXEL || blueType != Type.PIXEL) {
            throw new TypeCheckException("ExpandedPixelExpr components must have type PIXEL");
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
        for (int i = symbolTableStack.size() - 1; i >= 0; i--) {
            SymbolTable table = symbolTableStack.get(i);
            if (table.table.containsKey(identifierName)) {
                NameDef nameDef = table.table.get(identifierName);
                identExpr.setNameDef(nameDef); 
                identExpr.setType(nameDef.getType()); 
                System.out.println("Identifier found in scope: " + identifierName + ", Type: " + nameDef.getType());
                return nameDef.getType();
            }
        }

        if (root != null && root.getParams() != null) {
            for (NameDef param : root.getParams()) {
                if (param.getName().equals(identifierName)) {
                    identExpr.setNameDef(param);
                    identExpr.setType(param.getType()); 
                    System.out.println("Identifier found in parameters: " + identifierName + ", Type: " + param.getType());
                    return param.getType();
                }
            }
        }

        System.out.println("Identifier not found: " + identifierName);
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
        if (lValue.getNameDef() == null) {
            return null;
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
        Type type = Type.INT;
        numLitExpr.setType(type);
        System.out.println("Setting type of NumLitExpr to INT");
        return type;
    }


    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {

        pixelSelector.xExpr().visit(this, arg);
        pixelSelector.yExpr().visit(this, arg);

        return null;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {

        postfixExpr.primary().visit(this, arg);

        if (postfixExpr.pixel() != null) {
            postfixExpr.pixel().visit(this, arg);
        }

        if (postfixExpr.channel() != null) {
            postfixExpr.channel().visit(this, arg);
        }

        return null;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        System.out.println("Visiting Program: " + program.getName());
        root = program;
        Type type = Type.kind2type(program.getTypeToken().kind());
        program.setType(type);

        symbolTableStack.clear();
        symbolTableStack.push(new SymbolTable());
        System.out.println("Program parameters: " + program.getParams());

        List<NameDef> params = program.getParams();
        Set<String> paramNames = new HashSet<>();
        for (NameDef param : params) {
            if (!paramNames.add(param.getName())) {
                throw new TypeCheckException("Duplicate parameter name: " + param.getName());
            }
            symbolTableStack.peek().insert(param);
        }

        program.getBlock().visit(this, arg);

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
                return Type.INT;

            case BANG:
                if (exprType != Type.BOOLEAN) {
                    throw new TypeCheckException("Logical NOT (!) can only be applied to BOOLEAN expressions.");
                }
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
        if (exprType != Type.INT && exprType != Type.STRING && exprType != Type.BOOLEAN) {
            throw new PLCCompilerException("Invalid type for write statement: " + exprType);
        }
        return null;
    }


}
