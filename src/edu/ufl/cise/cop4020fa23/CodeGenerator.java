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

public class CodeGenerator implements ASTVisitor {

	private String packageName;
    private final SymbolTable symbolTable;
    Program root;
    private Stack<SymbolTable> symbolTableStack = new Stack<>();
    public CodeGenerator(String packageName, SymbolTable symbolTable) {
        this.packageName = packageName;
        this.symbolTable = symbolTable;
    }

    public CodeGenerator() {
        this.packageName = "defaultPackageName"; 
        this.symbolTable = new SymbolTable();    
    }


    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        StringBuilder sb;
        if (arg instanceof StringBuilder) {
            sb = (StringBuilder) arg;
        } else {
            sb = new StringBuilder();
            System.out.println("Debug: Creating a new StringBuilder for program code generation");
        }

        System.out.println("Debug: Visiting Program - Name: " + program.getName() + ", Type: " + program.getType());
        sb.append("package ").append(packageName).append(";\n");
        sb.append("public class ").append(program.getName()).append(" {\n");
        
        if ("f".equals(program.getName()) && Type.VOID.equals(program.getType())) {
            sb.append("public static void main(String[] args) {\n");
            sb.append("    new ").append(program.getName()).append("().f();\n");
            sb.append("}\n");
        }

        sb.append("public ").append(program.getType().toString().toLowerCase()).append(" f(");
        for (int i = 0; i < program.getParams().size(); i++) {
            System.out.println("Debug: Visiting Parameter " + (i + 1) + ": " + program.getParams().get(i).getName());
            program.getParams().get(i).visit(this, sb);
            if (i < program.getParams().size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(") {\n");
        program.getBlock().visit(this, sb);
        sb.append("}\n");
        sb.append("}\n");
        System.out.println("Debug: Finished Visiting Program - Generated Code:\n" + sb.toString());
        return null;
    }


    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting Block - Number of Elements: " + block.getElems().size());
        for (Block.BlockElem elem : block.getElems()) {
            elem.visit(this, sb);
        }
        return null;
    }


    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        Type type = Type.kind2type(nameDef.getTypeToken().kind());
        String javaType = switch (type) {
            case INT -> "int";
            case BOOLEAN -> "boolean";
            case STRING -> "String";
            default -> throw new UnsupportedOperationException("Unsupported type: " + type);
        };
        System.out.println("Debug: Visiting NameDef - Name: " + nameDef.getIdentToken().text() + ", Type: " + javaType);
        sb.append(javaType).append(" ").append(nameDef.getIdentToken().text());
        return null;
    }


    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting Declaration - Name: " + declaration.getNameDef().getName());
        declaration.getNameDef().visit(this, sb);
        if (declaration.getInitializer() != null) {
            sb.append(" = ");
            declaration.getInitializer().visit(this, sb);
        }
        sb.append(";\n");
        return null;
    }


    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting ConditionalExpr");

        sb.append("(");
        System.out.println("Debug: Guard Expression of ConditionalExpr");
        Type guardType = (Type) conditionalExpr.getGuardExpr().visit(this, sb);
        sb.append(" ? ");

        System.out.println("Debug: True Expression of ConditionalExpr");
        Type trueType = (Type) conditionalExpr.getTrueExpr().visit(this, sb);
        sb.append(" : ");

        System.out.println("Debug: False Expression of ConditionalExpr");
        Type falseType = (Type) conditionalExpr.getFalseExpr().visit(this, sb);
        sb.append(")");

        System.out.println("Debug: Resultant Type of ConditionalExpr: " + conditionalExpr.getType());
        return null;
    }


    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting BinaryExpr - Operator: " + binaryExpr.getOp().text());
        
        System.out.println("Debug: Left Expression of BinaryExpr");
        Type leftType = (Type) binaryExpr.getLeftExpr().visit(this, sb);
        sb.append(" ").append(binaryExpr.getOp().text()).append(" ");

        System.out.println("Debug: Right Expression of BinaryExpr");
        Type rightType = (Type) binaryExpr.getRightExpr().visit(this, sb);
        sb.append(")");
        
        System.out.println("Debug: Resultant Type of BinaryExpr: " + binaryExpr.getType());
        return null;
    }


    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting ConstExpr - Name: " + constExpr.getName());
        sb.append(constExpr.getName());
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting UnaryExpr - Operator: " + unaryExpr.getOp());
        sb.append("(");
        switch (unaryExpr.getOp()) {
            case BANG -> sb.append("!");
            case MINUS -> sb.append("-");
            default -> throw new UnsupportedOperationException("Unsupported unary operator: " + unaryExpr.getOp());
        }
        unaryExpr.getExpr().visit(this, sb);
        sb.append(")");
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting StringLitExpr - Text: " + stringLitExpr.getText());
        sb.append("\"").append(stringLitExpr.getText()).append("\"");
        return null;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting NumLitExpr - Value: " + numLitExpr.getText());
        sb.append(numLitExpr.getText());
        return null;
    }


    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        String identifierName = identExpr.getName();
        System.out.println("Debug: Visiting IdentExpr - Identifier: " + identifierName);
        boolean identifierFound = false;

        for (int i = symbolTableStack.size() - 1; i >= 0; i--) {
            SymbolTable table = symbolTableStack.get(i);
            if (table.table.containsKey(identifierName)) {
                sb.append(identifierName);
                identifierFound = true;
                break;
            }
        }

        if (!identifierFound && root != null && root.getParams() != null) {
            for (NameDef param : root.getParams()) {
                if (param.getName().equals(identifierName)) {
                    sb.append(identifierName);
                    identifierFound = true;
                    break;
                }
            }
        }

        if (!identifierFound) {
            sb.append(identifierName);
        }

        return null;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting BooleanLitExpr - Value: " + booleanLitExpr.getText());
        sb.append(booleanLitExpr.getText());
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting WriteStatement");
        sb.append("System.out.println(");
        writeStatement.getExpr().visit(this, sb);
        sb.append(");\n");
        return null;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting BlockStatement");
        statementBlock.getBlock().visit(this, sb);
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting ReturnStatement");
        sb.append("return ");
        returnStatement.getE().visit(this, sb);
        sb.append(";\n");
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting AssignmentStatement");

        System.out.println("Debug: LValue of AssignmentStatement");
        Type lValueType = (Type) assignmentStatement.getlValue().visit(this, sb);
        sb.append(" = ");

        System.out.println("Debug: Expression of AssignmentStatement");
        Type exprType = (Type) assignmentStatement.getE().visit(this, sb);
        sb.append(";\n");

        System.out.println("Debug: LValue Type: " + lValueType + ", Expression Type: " + exprType);
        return null;
    }



    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting LValue - Name: " + lValue.getName());
        sb.append(lValue.getName()); 
        return null;
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting ChannelSelector - Color: " + channelSelector.color().name());
        sb.append(channelSelector.color().name());
        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting Dimension");
        sb.append("new Dimension(");
        dimension.getWidth().visit(this, sb);
        sb.append(", ");
        dimension.getHeight().visit(this, sb);
        sb.append(")");
        return null;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting DoStatement");
        sb.append("do {\n");
        for (GuardedBlock guardedBlock : doStatement.getGuardedBlocks()) {
            guardedBlock.visit(this, sb);
        }
        sb.append("} while (/* condition */);\n");
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting ExpandedPixelExpr");
        sb.append("new Color(");
        expandedPixelExpr.getRed().visit(this, sb);
        sb.append(", ");
        expandedPixelExpr.getGreen().visit(this, sb);
        sb.append(", ");
        expandedPixelExpr.getBlue().visit(this, sb);
        sb.append(")");
        return null;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting GuardedBlock");
        sb.append("if (");
        guardedBlock.getGuard().visit(this, sb);
        sb.append(") {\n");
        guardedBlock.getBlock().visit(this, sb);
        sb.append("}\n");
        return null;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting IfStatement");
        for (GuardedBlock guardedBlock : ifStatement.getGuardedBlocks()) {
            sb.append("if (");
            guardedBlock.getGuard().visit(this, sb);
            sb.append(") {\n");
            guardedBlock.getBlock().visit(this, sb);
            sb.append("}\n");
        }
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting PixelSelector");
        sb.append("selectPixel(");
        pixelSelector.xExpr().visit(this, sb);
        sb.append(", ");
        pixelSelector.yExpr().visit(this, sb);
        sb.append(")");
        return null;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting PostfixExpr");
        postfixExpr.primary().visit(this, sb);
        if (postfixExpr.pixel() != null) {
            postfixExpr.pixel().visit(this, sb);
        }
        if (postfixExpr.channel() != null) {
            postfixExpr.channel().visit(this, sb);
        }
        return null;
    }


}
