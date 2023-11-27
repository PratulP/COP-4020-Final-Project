package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
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

public class CodeGenerator implements ASTVisitor {
    private String packageName;
    private final SymbolTable symbolTable;
    private Program root;
    private Stack<SymbolTable> symbolTableStack = new Stack<>();
    private int uniqueCounter = 1;
    private Map<String, String> parameterNames = new HashMap<>();

    public CodeGenerator(String packageName) {
        this.packageName = (packageName == null || packageName.equals("defaultPackageName") || packageName.isEmpty()) ? null : packageName;
        this.symbolTable = new SymbolTable();
    }

   
    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        StringBuilder sb = new StringBuilder();

        sb.append("package edu.ufl.cise.cop4020fa23;\n");

        String className = program.getName();
        String fullyQualifiedName = (this.packageName != null && !this.packageName.isEmpty()) 
                                    ? this.packageName + "." + className 
                                    : className;

        sb.append("import edu.ufl.cise.cop4020fa23.ConsoleIO;\n");

        sb.append("public class ").append(fullyQualifiedName).append(" {\n");

        String returnType = getJavaType(program.getType());
        sb.append("    public static ").append(returnType).append(" apply(");

        List<NameDef> params = program.getParams();
        for (int i = 0; i < params.size(); i++) {
            NameDef param = params.get(i);
            param.visit(this, sb);
            if (i < params.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(") {\n");

        program.getBlock().visit(this, sb);

        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
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


    


    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
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
        StringBuilder sb = (StringBuilder)arg;
        if (sb == null) {
            throw new IllegalStateException("StringBuilder object is null in visitBinaryExpr");
        }

        if (binaryExpr.getOpKind() == Kind.EXP) {
            sb.append("(int)Math.pow(");
            binaryExpr.getLeftExpr().visit(this, sb);
            sb.append(", ");
            binaryExpr.getRightExpr().visit(this, sb);
            sb.append(")");
        } else if (binaryExpr.getOpKind() == Kind.PLUS &&
                   binaryExpr.getLeftExpr().getType() == Type.STRING &&
                   binaryExpr.getRightExpr().getType() == Type.STRING) {
            binaryExpr.getLeftExpr().visit(this, sb);
            sb.append(" + ");
            if (binaryExpr.getRightExpr() instanceof StringLitExpr) {
                StringLitExpr rightExpr = (StringLitExpr) binaryExpr.getRightExpr();
                sb.append(rightExpr.getText());
            } else {
                binaryExpr.getRightExpr().visit(this, sb);
            }
        } else if (binaryExpr.getOpKind() == Kind.EQ &&
                   binaryExpr.getLeftExpr().getType() == Type.STRING &&
                   binaryExpr.getRightExpr().getType() == Type.STRING) {
            binaryExpr.getLeftExpr().visit(this, sb);
            sb.append(".equals(");
            binaryExpr.getRightExpr().visit(this, sb);
            sb.append(")");
        } else {
            if (binaryExpr.getLeftExpr() instanceof BinaryExpr) {
                sb.append("(");
                binaryExpr.getLeftExpr().visit(this, sb);
                sb.append(")");
            } else {
                binaryExpr.getLeftExpr().visit(this, sb);
            }
            sb.append(" ").append(binaryExpr.getOp().text()).append(" ");
            if (binaryExpr.getRightExpr() instanceof BinaryExpr) {
                sb.append("(");
                binaryExpr.getRightExpr().visit(this, sb);
                sb.append(")");
            } else {
                binaryExpr.getRightExpr().visit(this, sb);
            }
        }

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

        if (unaryExpr.getOp() == Kind.MINUS) {
            sb.append("(-("); 
            unaryExpr.getExpr().visit(this, sb); 
            sb.append("))"); 
        } else {
            
            sb.append("(");
            switch (unaryExpr.getOp()) {
                case BANG -> sb.append("!");
                default -> throw new UnsupportedOperationException("Unsupported unary operator: " + unaryExpr.getOp());
            }
            unaryExpr.getExpr().visit(this, sb);
            sb.append(")");
        }

        return null;
    }


    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        sb.append(stringLitExpr.getText()); 
        return null;
    }



    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting NumLitExpr - Value: " + numLitExpr.getText());
        sb.append(numLitExpr.getText());
        return null;
    }
    
   
    
    private String getJavaType(Type type) {
        return switch (type) {
            case INT -> "int";
            case BOOLEAN -> "boolean";
            case STRING -> "String";
            case VOID -> "void"; 
            default -> throw new UnsupportedOperationException("Unsupported type: " + type);
        };
    }


    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        String javaType = getJavaType(nameDef.getType());
        String originalName = nameDef.getIdentToken().text();
        String uniqueName = getUniqueName(originalName);

        parameterNames.put(originalName, uniqueName);

        sb.append(javaType).append(" ").append(uniqueName);
        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        if (sb == null) {
            throw new IllegalStateException("StringBuilder object is null in visitIdentExpr");
        }

        String originalName = identExpr.getName();
        String identifierName = parameterNames.getOrDefault(originalName, getUniqueName(originalName));

        sb.append(identifierName);
        return null;
    }


    private String getUniqueName(String name) {
        if (!parameterNames.containsKey(name)) {
            return name + "_" + uniqueCounter++;
        }
        return parameterNames.get(name);
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        String literalValue = booleanLitExpr.getText().equals("TRUE") ? "true" : "false";
        System.out.println("Debug: Visiting BooleanLitExpr - Value: " + literalValue);
        sb.append(literalValue);
        return null;
    }



    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        sb.append("ConsoleIO.write(");
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

    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        sb.append("return ");
        returnStatement.getE().visit(this, sb);
        sb.append(";\n");
        return null;
    }



    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        assignmentStatement.getlValue().visit(this, sb);
        sb.append(" = ");
        assignmentStatement.getE().visit(this, sb);
        sb.append(";\n");
        return null;
    }



    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        String uniqueName = getUniqueName(lValue.getName());
        sb.append(uniqueName);
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
