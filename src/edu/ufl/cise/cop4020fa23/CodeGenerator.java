package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;

import edu.ufl.cise.cop4020fa23.runtime.PixelOps;
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
        sb.append("import java.awt.image.BufferedImage;\n"); 
        sb.append("import edu.ufl.cise.cop4020fa23.runtime.ImageOps;\n"); 
        sb.append("import edu.ufl.cise.cop4020fa23.runtime.FileURLIO;\n");
        sb.append("import java.awt.Color;\n");
        sb.append("import edu.ufl.cise.cop4020fa23.runtime.PixelOps;\n");
        sb.append("import edu.ufl.cise.cop4020fa23.runtime.ConsoleIO;\n");

        String className = program.getName();
        String fullyQualifiedName = (this.packageName != null && !this.packageName.isEmpty()) 
                                    ? this.packageName + "." + className 
                                    : className;

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
        
        System.out.println("Debug: Generating Java code for program: " + program.getName());

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
    
    
    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        NameDef nameDef = declaration.getNameDef();
        Expr initializer = declaration.getInitializer();
        Dimension dim = nameDef.getDimension();
        nameDef.visit(this, sb);

       if (nameDef.getType() == Type.IMAGE) {
            if (dim != null) {
                if (initializer instanceof IdentExpr) {
                    String imageUrl = ((IdentExpr) initializer).getName();
                    sb.append(" = FileURLIO.readImage(").append(parameterNames.getOrDefault(imageUrl, imageUrl))
                          .append(", ").append(dim.getWidth()).append(", ").append(dim.getHeight()).append(");");
                } else {
                   sb.append(" = new BufferedImage(");
                    dim.getWidth().visit(this, sb);
                    sb.append(", ");
                    dim.getHeight().visit(this, sb);
                    sb.append(", BufferedImage.TYPE_INT_ARGB);");
                }
            } else if (initializer != null && initializer instanceof IdentExpr) {
                String imageUrl = ((IdentExpr) initializer).getName();
                sb.append(" = FileURLIO.readImage(").append(parameterNames.getOrDefault(imageUrl, imageUrl)).append(");");
            }
        } else if (declaration.getInitializer() != null) {
            sb.append(" = ");
            declaration.getInitializer().visit(this, sb);
        }
        sb.append(";\n");
        return null;



    /*    if (nameDef.getType() == Type.IMAGE) {
            if (dim == null) {
                sb.append(" ");
                declaration.getInitializer().visit(this,sb);
                sb.append(");\n");
                if (initializer.getType() == Type.STRING) {
                    declaration.getInitializer().visit(this,sb);
                    sb.append(" = FileURLIO.readImage(");
                    nameDef.visit(this, sb);
                    sb.append(");\n");
                }
                else if (initializer.getType() == Type.IMAGE) {
                    declaration.getInitializer().visit(this,sb);
                    sb.append(" = ImageOps.cloneImage(");
                    nameDef.visit(this, sb);
                    sb.append(");\n");
                }}
        } else if (nameDef.getType() == Type.PIXEL) {
            sb.append(" ");
            declaration.getInitializer().visit(this,sb);
            sb.append(");\n");
            if (dim != null) {
                declaration.getInitializer().visit(this,sb);
                sb.append(" = ");
                nameDef.visit(this,sb);
                sb.append(");\n");
            }
        }
        else {
                if (initializer == null) {
                    sb.append(" ");
                    declaration.getInitializer().visit(this,sb);
                    sb.append(");\n");
                    declaration.getInitializer().visit(this,sb);
                    sb.append(" = ImageOps.makeImage(");
                   dim.getWidth().visit(this,sb);
                   sb.append(", ");
                   dim.getHeight().visit(this, sb);
                    sb.append(");\n");
                }
                else {
                    if (initializer.getType() == Type.STRING) {
                        sb.append(" ");
                        declaration.getInitializer().visit(this,sb);
                        sb.append(");\n");
                        declaration.getInitializer().visit(this,sb);
                        sb.append(" = FileURLIO.readImage(");
                        nameDef.visit(this,sb);
                        sb.append(", ");
                        dim.getWidth().visit(this,sb);
                        sb.append(", ");
                        dim.getHeight().visit(this, sb);
                        sb.append(");\n");
                    }
                    else if (initializer.getType() == Type.PIXEL) {
                        sb.append(" ");
                        declaration.getInitializer().visit(this,sb);
                        sb.append(");\n");
                        declaration.getInitializer().visit(this,sb);
                        sb.append(" = ImageOps.makeImage(");
                        dim.getWidth().visit(this,sb);
                        sb.append(", ");
                        dim.getHeight().visit(this, sb);
                        sb.append(");\n");
                        sb.append("ImageOps.setAllPixels(");
                        declaration.getInitializer().visit(this,sb);
                        sb.append(", ");
                        nameDef.visit(this,sb);
                        sb.append(");\n");
                    } else if (initializer.getType() == Type.IMAGE) {
                        sb.append(" ");
                        declaration.getInitializer().visit(this,sb);
                        sb.append(");\n");
                        declaration.getInitializer().visit(this,sb);
                        sb.append(" = ImageOps.copyAndResize(");
                        nameDef.visit(this,sb);
                        sb.append(", ");
                        dim.getWidth().visit(this,sb);
                        sb.append(", ");
                        dim.getHeight().visit(this, sb);
                        sb.append(");\n");
                    }
                }
            }
        return null;*/
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

        Type leftType = binaryExpr.getLeftExpr().getType();
        Type rightType = binaryExpr.getRightExpr().getType();



        if (binaryExpr.getOpKind() == Kind.PLUS && leftType == Type.IMAGE && rightType == Type.IMAGE) {
            sb.append("ImageOps.binaryImageImageOp(ImageOps.OP.PLUS, ");
            binaryExpr.getLeftExpr().visit(this, sb);
            sb.append(", ");
            binaryExpr.getRightExpr().visit(this, sb);
            sb.append(")");
        }
        else if (binaryExpr.getOpKind() == Kind.EQ && leftType == Type.IMAGE && rightType == Type.IMAGE) {
            sb.append("ImageOps.binaryPackedPixelBooleanOp(ImageOps.BoolOP.EQUALS,");
            binaryExpr.getLeftExpr().visit(this, sb);
            sb.append(", ");
            binaryExpr.getRightExpr().visit(this,sb);
            sb.append(")");
        } else if (binaryExpr.getOpKind() == Kind.EQ && leftType == Type.IMAGE && rightType == Type.INT) {
            sb.append("ImageOps.binaryPackedPixelBooleanOp(ImageOps.BoolOP.EQUALS,");
            binaryExpr.getLeftExpr().visit(this, sb);
            sb.append(", ");
            binaryExpr.getRightExpr().visit(this,sb);
            sb.append(")");
        } else if (binaryExpr.getOpKind() == Kind.EQ && leftType == Type.PIXEL && rightType == Type.PIXEL) {
            sb.append("ImageOps.binaryPackedPixelBooleanOp(ImageOps.BoolOP.EQUALS,");
            binaryExpr.getLeftExpr().visit(this, sb);
            sb.append(", ");
            binaryExpr.getRightExpr().visit(this, sb);
            sb.append(")");
        }
        else if (binaryExpr.getOpKind() == Kind.EXP) {
            sb.append("((int)Math.round(Math.pow(");
            binaryExpr.getLeftExpr().visit(this, sb);
            sb.append(", ");
            binaryExpr.getRightExpr().visit(this, sb);
            sb.append(")))");
        } else if (binaryExpr.getOpKind() == Kind.TIMES && (leftType == Type.IMAGE && rightType == Type.INT)) {
            sb.append("ImageOps.binaryImageScalarOp(ImageOps.OP.TIMES, ");
            binaryExpr.getLeftExpr().visit(this, sb);
            sb.append(", ");
            binaryExpr.getRightExpr().visit(this, sb);
            sb.append(")");
        } else if (binaryExpr.getOpKind() == Kind.TIMES && (leftType == Type.PIXEL && rightType == Type.INT || leftType == Type.INT && rightType == Type.PIXEL)) {
            sb.append("ImageOps.binaryPackedPixelIntOp(ImageOps.OP.TIMES, ");
            binaryExpr.getLeftExpr().visit(this, sb);
            sb.append(", ");
            binaryExpr.getRightExpr().visit(this, sb);
            sb.append(")");
        } else if (binaryExpr.getOpKind() == Kind.DIV && (leftType == Type.PIXEL && rightType == Type.INT)) {
            sb.append("ImageOps.binaryPackedPixelIntOp(ImageOps.OP.DIV, ");
            binaryExpr.getLeftExpr().visit(this, sb);
            sb.append(", ");
            binaryExpr.getRightExpr().visit(this, sb);
            sb.append(")");
        } else if (leftType == Type.PIXEL || rightType == Type.PIXEL) {
            sb.append("ImageOps.binaryPackedPixelPixelOp(ImageOps.OP.");
            sb.append(binaryExpr.getOpKind().name());
            sb.append(", ");
            binaryExpr.getLeftExpr().visit(this, sb);
            sb.append(", ");
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

        switch (constExpr.getName()) {
            case "RED":
                sb.append("Color.RED.getRGB()");
                break;
            case "GREEN":
                sb.append("Color.GREEN.getRGB()");
                break;
            case "BLUE":
                sb.append("Color.BLUE.getRGB()");
                break;
            case "PINK":
                sb.append("Color.PINK.getRGB()");
                break;
            case "LIGHT_GRAY":
                sb.append("Color.LIGHT_GRAY.getRGB()");
                break;
            case "WHITE":
                sb.append("Color.WHITE.getRGB()");
                break;
            case "BLACK":
                sb.append("Color.BLACK.getRGB()");
                break;
            case "CYAN":
                sb.append("Color.CYAN.getRGB()");
                break;
            case "Z":
                sb.append("255");
                break;
            default:
                throw new PLCCompilerException("Undefined constant: " + constExpr.getName());
        }
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
        } else if (unaryExpr.getOp() == Kind.RES_width) {
            sb.append("(");
            unaryExpr.getExpr().visit(this, sb);  
            sb.append(").getWidth()");            
        } else if (unaryExpr.getOp() == Kind.RES_height) {
            sb.append("(");
            unaryExpr.getExpr().visit(this, sb);  
            sb.append(").getHeight()");         
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
            case IMAGE -> "BufferedImage";
            case PIXEL -> "int";
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
        Type exprType = writeStatement.getExpr().getType();

        sb.append("ConsoleIO.write(");
        if (exprType == Type.PIXEL) {
            sb.append("Integer.toHexString(");
            writeStatement.getExpr().visit(this, sb);
            sb.append(")");
        } else {
            writeStatement.getExpr().visit(this, sb);
        }
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
        StringBuilder sb = (StringBuilder)arg;
        LValue lValue = assignmentStatement.getlValue();
        Expr expr = assignmentStatement.getE();
        
        System.out.println("Debug: Processing assignment to " + lValue.getName());


        lValue.visit(this, sb);

        Type lValueType = lValue.getVarType();
        Type exprType = expr.getType();

        if (lValueType == Type.IMAGE) {
            NameDef nameDef = lValue.getNameDef();
            if (nameDef != null && nameDef.getDimension() != null) {
                Dimension dim = nameDef.getDimension();
                if (exprType == Type.PIXEL || expr instanceof ExpandedPixelExpr) {
                    sb.append(" = ImageOps.setAllPixels(new BufferedImage(");
                    dim.getWidth().visit(this, sb);
                    sb.append(", ");
                    dim.getHeight().visit(this, sb);
                    sb.append(", BufferedImage.TYPE_INT_ARGB), ");
                    expr.visit(this, sb);
                    sb.append(");");
                } else if (exprType == Type.STRING) {
                    sb.append(" = FileURLIO.readImage(");
                    expr.visit(this, sb);
                    sb.append(", ");
                    dim.getWidth().visit(this, sb);
                    sb.append(", ");
                    dim.getHeight().visit(this, sb);
                    sb.append(");");
                } else if (expr instanceof BinaryExpr && lValue.getPixelSelector() != null) {
                    sb.append(" = ImageOps.makeImage(");
                    dim.getWidth().visit(this, sb);
                    sb.append(", ");
                    dim.getHeight().visit(this, sb);
                    sb.append(");\nfor (int x = 0; x < ");
                    dim.getWidth().visit(this, sb);
                    sb.append("; x++) {\n    for (int y = 0; y < ");
                    dim.getHeight().visit(this, sb);
                    sb.append("; y++) {\n        ImageOps.setRGB(");
                    lValue.visit(this, sb); 
                    sb.append(", x, y, ");
                    expr.visit(this, sb);
                    sb.append(");\n    }\n}\n");
                } else {
                    throw new PLCCompilerException("Unsupported expression type for image initialization: " + expr.getClass().getSimpleName());
                }
            } else {
                sb.append(" = ");
                expr.visit(this, sb);
            }
        } else if (expr instanceof PostfixExpr && ((PostfixExpr) expr).channel() != null 
                && ((PostfixExpr) expr).channel().color() == Kind.RES_red) {
            sb.append(" = PixelOps.red(ImageOps.getRGB(");
            ((PostfixExpr) expr).primary().visit(this, sb);
            sb.append(", ");
            ((PostfixExpr) expr).pixel().xExpr().visit(this, sb);
            sb.append(", ");
            ((PostfixExpr) expr).pixel().yExpr().visit(this, sb);
            sb.append("))");
        } else {
            sb.append(" = ");
            expr.visit(this, sb);
        }
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
        StringBuilder sb = (StringBuilder)arg;
        sb.append("boolean continueLoop = true;\n"); 
        sb.append("do {\n");

        sb.append("    continueLoop = false;\n");

        for (GuardedBlock guardedBlock : doStatement.getGuardedBlocks()) {
            sb.append("    if (");
            guardedBlock.getGuard().visit(this, sb);
            sb.append(") {\n");
            guardedBlock.getBlock().visit(this, sb);

            boolean containsReturn = false;
            for (Block.BlockElem elem : guardedBlock.getBlock().getElems()) {
                if (elem instanceof ReturnStatement) {
                    containsReturn = true;
                    break;
                }
            }

            if (!containsReturn) {
                sb.append("        continueLoop = true;\n");
            }
            sb.append("    }\n");
        }

        sb.append("} while (continueLoop);\n");  
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        System.out.println("Debug: Visiting ExpandedPixelExpr");
        sb.append("PixelOps.pack(");
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

        if (pixelSelector.xExpr() instanceof IdentExpr && ((IdentExpr) pixelSelector.xExpr()).getNameDef() instanceof SyntheticNameDef &&
            pixelSelector.yExpr() instanceof IdentExpr && ((IdentExpr) pixelSelector.yExpr()).getNameDef() instanceof SyntheticNameDef) {

            String imageVarName = "image"; 

            sb.append("for (int x = 0; x < ").append(imageVarName).append(".getWidth(); x++) {\n");
            sb.append("    for (int y = 0; y < ").append(imageVarName).append(".getHeight(); y++) {\n");
            sb.append("    }\n");
            sb.append("}\n");
        } else {
            sb.append("selectPixel(");
            pixelSelector.xExpr().visit(this, sb);
            sb.append(", ");
            pixelSelector.yExpr().visit(this, sb);
            sb.append(")");
        }
        return null;
    }


    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        StringBuilder sb = (StringBuilder) arg;
        Expr primaryExpr = postfixExpr.primary();
        PixelSelector pixelSelector = postfixExpr.pixel();
        ChannelSelector channelSelector = postfixExpr.channel();

        if (pixelSelector != null && channelSelector != null) {
            switch (channelSelector.color()) {
                case RES_red -> sb.append("PixelOps.red(");
                case RES_green -> sb.append("PixelOps.green(");
                case RES_blue -> sb.append("PixelOps.blue(");
                default -> throw new UnsupportedOperationException("Unsupported channel selector: " + channelSelector.color());}
            sb.append("ImageOps.getRGB(");
            primaryExpr.visit(this,sb);
            sb.append(", ");
            pixelSelector.xExpr().visit(this, sb);
            sb.append(", ");
            pixelSelector.yExpr().visit(this, sb);
            sb.append("))");
        } else if (pixelSelector != null && channelSelector == null) {
            sb.append("ImageOps.getRGB(");
            primaryExpr.visit(this, sb);
            sb.append(", ");
            pixelSelector.xExpr().visit(this, sb);
            sb.append(", ");
            pixelSelector.yExpr().visit(this, sb);
            sb.append(")");
        } else {
            if (primaryExpr.getType() == Type.IMAGE) {
                String channelMethod = "";
                switch (channelSelector.color()) {
                    case RES_red -> channelMethod = "extractRed";
                    case RES_green -> channelMethod = "extractGreen";
                    case RES_blue -> channelMethod = "extractBlue";
                    default -> throw new UnsupportedOperationException("Unsupported channel selector: " + channelSelector.color());
                }
                sb.append("ImageOps.").append(channelMethod).append("(");
                primaryExpr.visit(this, sb);
                sb.append(")");
            } else if (primaryExpr.getType() == Type.PIXEL) {
                switch (channelSelector.color()) {
                    case RES_red -> sb.append("PixelOps.red(");
                    case RES_green -> sb.append("PixelOps.green(");
                    case RES_blue -> sb.append("PixelOps.blue(");
                    default -> throw new UnsupportedOperationException("Unsupported channel selector: " + channelSelector.color());
                }
                primaryExpr.visit(this, sb);
                sb.append(")");
            } else {
                throw new UnsupportedOperationException("Unsupported type for channel selector: " + primaryExpr.getType());
            }
        }
        return null;
    }
}
