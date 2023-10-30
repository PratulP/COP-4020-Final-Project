package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;

public class TypeCheckVisitor implements ASTVisitor {
    private SymbolTable symbolTable = new SymbolTable();

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        symbolTable.enterScope();

        Expr e = assignmentStatement.getE();

        LValue lValue = assignmentStatement.getlValue();

        Type lValueType = (Type) lValue.visit(this, arg);

        Type exprType = (Type) e.visit(this, arg);

        if (lValueType == Type.IMAGE) {
            if (exprType != Type.PIXEL && exprType != Type.INT) {
                throw new PLCCompilerException("Assignment type mismatch");
            }
        } else if (lValueType != exprType) {
            throw new PLCCompilerException("Assignment type mismatch");
        }

        symbolTable.leaveScope();
        return null;
    }


    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {

        int left = (int)binaryExpr.getLeftExpr().visit(this, arg);
        int right = (int)binaryExpr.getRightExpr().visit(this,arg);
        Kind opKind = binaryExpr.getOpKind();
        int val = switch(opKind) {
            case PLUS -> left + right;
            case MINUS -> left - right;
            case TIMES -> left * right;
            case DIV -> left / right;
            default -> throw new PLCCompilerException();
        };
        return val;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        symbolTable.enterScope();
        for (Block.BlockElem elem : block.getElems()) {
            elem.visit(this, arg);
        }
        symbolTable.leaveScope();
        //return null;
        return block;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        String boolText = booleanLitExpr.getText();

        boolean boolValue;
        if (boolText.equals("true")) {
            boolValue = true;
        } else if (boolText.equals("false")) {
            boolValue = false;
        } else {
            throw new PLCCompilerException("Invalid boolean literal: " + boolText);
        }

        return boolValue;
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

        return constType;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        NameDef nameDef = declaration.getNameDef();
        Expr initializer = declaration.getInitializer();

        if (initializer == null) {
            Type declaredType = nameDef.getType();
            if (declaredType != Type.INT && declaredType != Type.BOOLEAN && declaredType != Type.STRING) {
                throw new PLCCompilerException("Invalid type for declaration: " + declaredType);
            }
        } else {
            Type declaredType = nameDef.getType();
            Type initializedType = (Type) initializer.visit(this, arg);

            if (declaredType != initializedType) {
                throw new PLCCompilerException("Type mismatch in declaration");
            }
        }

        symbolTable.insert(nameDef);

        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        Type widthType = (Type) dimension.getWidth().visit(this, arg);
        Type heightType = (Type) dimension.getHeight().visit(this, arg);

        if (widthType != Type.INT || heightType != Type.INT) {
            throw new PLCCompilerException("Dimension width and height must be of type INT");
        }

        return dimension;
       // return null;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        if (doStatement.getGuardedBlocks().isEmpty()) {
            throw new PLCCompilerException("Do statement must have at least one guarded block");
        }

        for (GuardedBlock guardedBlock : doStatement.getGuardedBlocks()) {
            guardedBlock.visit(this, arg);
        }

        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        Type redType = (Type) expandedPixelExpr.getRed().visit(this, arg);
        Type greenType = (Type) expandedPixelExpr.getGreen().visit(this, arg);
        Type blueType = (Type) expandedPixelExpr.getBlue().visit(this, arg);

        if (redType != Type.PIXEL || greenType != Type.PIXEL || blueType != Type.PIXEL) {
            throw new PLCCompilerException("ExpandedPixelExpr components must have type PIXEL");
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
        if (identExpr.getNameDef() == null) {
            throw new PLCCompilerException("Undefined identifier: " + identExpr.getName());
        }

        return identExpr.getNameDef().getType();
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        if (ifStatement.getGuardedBlocks().isEmpty()) {
            throw new PLCCompilerException("If statement must have at least one guarded block");
        }

        for (GuardedBlock guardedBlock : ifStatement.getGuardedBlocks()) {
            guardedBlock.visit(this, arg);
        }

        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        if (lValue.getNameDef() == null) {
            throw new PLCCompilerException("Undefined identifier: " + lValue.getName());
        }

        if (lValue.getPixelSelector() != null) {
            lValue.getPixelSelector().visit(this, arg);

            if (lValue.getVarType() != Type.IMAGE) {
                throw new PLCCompilerException("Pixel selector can only be used with IMAGE type");
            }
        }

        if (lValue.getChannelSelector() != null) {
            lValue.getChannelSelector().visit(this, arg);

            if (lValue.getVarType() != Type.PIXEL && lValue.getVarType() != Type.IMAGE) {
                throw new PLCCompilerException("Channel selector can only be used with PIXEL or IMAGE type");
            }
        }

        return lValue.getVarType();
    }
    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {

        Type declaredType = nameDef.getType();

        if (declaredType != Type.INT && declaredType != Type.BOOLEAN && declaredType != Type.STRING) {
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

       /* String numText = numLitExpr.getText();

        try {
            Integer.parseInt(numText);
        } catch (NumberFormatException e) {
            throw new PLCCompilerException("Invalid numeric literal: " + numText);
        }

        return null;*/

        Type type = Type.INT;
        numLitExpr.setType(type);
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
        /*program.getBlock().visit(this, arg);
        return null;*/

        Type type = Type.kind2type(program.getTypeToken().kind());
        program.setType(type);
        symbolTable.enterScope();
        List<NameDef> params = program.getParams();
        for (NameDef param : params) {
            param.visit(this, arg);
        }
        program.getBlock().visit(this, arg);
        symbolTable.leaveScope();
        return type;
    }


    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {

        if (returnStatement.getE() != null) {

            returnStatement.getE().visit(this, arg);

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
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {

        unaryExpr.getExpr().visit(this, arg);

        switch (unaryExpr.getOp()) {
            case MINUS:
                if (unaryExpr.getExpr().getType() != Type.INT) {
                    throw new PLCCompilerException("Unary minus (-) can only be applied to INT expressions.");
                }
                break;
            case BANG:
                if (unaryExpr.getExpr().getType() != Type.BOOLEAN) {
                    throw new PLCCompilerException("Logical NOT (!) can only be applied to BOOLEAN expressions.");
                }
                break;
            case RES_width:
            case RES_height:
                if (unaryExpr.getExpr().getType() != Type.IMAGE) {
                    throw new PLCCompilerException("RES_width and RES_height can only be applied to IMAGE expressions.");
                }
                break;
        }


        return null;
    }
    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {

        writeStatement.getExpr().visit(this, arg);


        if (writeStatement.getExpr().getType() != Type.INT
                && writeStatement.getExpr().getType() != Type.STRING
                && writeStatement.getExpr().getType() != Type.BOOLEAN) {
            throw new PLCCompilerException("Invalid type for write statement: " + writeStatement.getExpr().getType());
        }

        //return null;
        return writeStatement;
    }


}
