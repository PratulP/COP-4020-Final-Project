/*Copyright 2023 by Beverly A Sanders
 *
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the
 * University of Florida during the fall semester 2023 as part of the course project.
 *
 * No other use is authorized.
 *
 * This code may not be posted on a public web site either during or after the course.
 */
package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import static edu.ufl.cise.cop4020fa23.Kind.*;


import java.lang.reflect.Type;
import java.util.*;

public class Parser implements IParser {

    final ILexer lexer;
    private IToken t;

    public Parser(ILexer lexer) throws LexicalException {
        super();
        this.lexer = lexer;
        t = lexer.next();
    }

    @Override
    public AST parse() throws PLCCompilerException {
        AST e = program();
        return e;
    }

    private Program program() throws PLCCompilerException {
        IToken firstToken = t;
        IToken typeToken = null;
        IToken nameToken = null;

        if (Arrays.asList(RES_image, RES_pixel, RES_int, RES_string, RES_void, RES_boolean).contains(t.kind())) {
            typeToken = t;
            consume();
        } else {

            throw new PLCCompilerException("Expected a valid type");
        }


        if (t.kind() == IDENT) {
            nameToken = t;
            consume();
        } else {

            throw new SyntaxException("Expected an IDENT");
        }

        match(LPAREN);
        ArrayList<NameDef> paramList = param_list();
        match(RPAREN);

        Block block = null;

        if (t.kind() == BLOCK_OPEN) {
            block = block();
        }

        if (t.kind() != EOF) {
        throw new SyntaxException("Issue");
        } else {
            return new Program(firstToken, typeToken, nameToken, paramList, block);
        }

    }

    private Block block() throws PLCCompilerException {
        IToken firstToken = t;
        List<Block.BlockElem> elems = new ArrayList<>();
        match(BLOCK_OPEN);
        if (t.kind() == EOF) {
            throw new SyntaxException("Incomplete block, unexpected end of file");
        }

        while (t.kind() != BLOCK_CLOSE) {
            if (t.kind() == SEMI) {
                consume(); 
                continue; 
            }
            if (Arrays.asList(RES_int, RES_string, RES_boolean, RES_image, RES_pixel).contains(t.kind())) {
                elems.add(declaration());
            } else {
                elems.add(statement());
            }
            match(SEMI);
        }
        match(BLOCK_CLOSE);
        return new Block(firstToken, elems);
    }

    private ArrayList<NameDef> param_list() throws PLCCompilerException {
        ArrayList<NameDef> params = new ArrayList<>();

        while (t.kind() != RPAREN) {
            if (t.kind() == EOF) {
                throw new SyntaxException("Unexpected end of file while parsing parameter list");
            }

            if (t.kind() == COMMA) {
                consume(); 
            } else if (params.size() > 0) {
                throw new SyntaxException("Expected a comma between parameters");
            }

            NameDef param = nameDef();
            params.add(param);
        }

        return params;
    }

    private NameDef nameDef() throws PLCCompilerException {
        IToken firstToken = t;
        IToken typeToken = t;
        Type type = type();

        Dimension dimension = null;
        if (t.kind() == LSQUARE) {
           
            dimension = dimension();
        }

        if (t.kind() != IDENT) {
            throw new SyntaxException("Expected identifier, found " + t.kind());
        }
        IToken identToken = t;
        match(IDENT); 

        return new NameDef(firstToken, typeToken, dimension, identToken);
    }


    enum Type {
        IMAGE,
        PIXEL,
        INT,
        STRING,
        BOOLEAN,
        VOID,
        RES_image, 
        RES_pixel
    }

    private Type type() throws PLCCompilerException {
        IToken firstToken = t;
        Type type = null;

        switch (t.kind()) {
            case RES_image: 
                type = Type.IMAGE;
                match(RES_image);
                break;
            case RES_pixel:
                type = Type.PIXEL;
                match(RES_pixel);
                break;
            case RES_int:
                type = Type.INT;
                match(RES_int);
                break;
            case RES_string:
                type = Type.STRING;
                match(RES_string);
                break;
            case IDENT:
                if (t.text().equals("void")) {
                    type = Type.VOID;
                    match(IDENT);
                } else {
                    throw new TypeCheckException("Expected a valid type");
                }
                break;
            case RES_boolean:
                type = Type.BOOLEAN;
                match(RES_boolean);
                break;
            default:
                throw new TypeCheckException("Expected a valid type");
        }

        return type;
    }


    private Declaration declaration() throws PLCCompilerException {
        IToken firstToken = t;
        NameDef nameDef = nameDef();
        Expr initializer = null;

        if (t.kind() == ASSIGN) {
            match(ASSIGN);
            initializer = expr();
        }


        return new Declaration(firstToken, nameDef, initializer);
    }


    private Expr expr() throws PLCCompilerException {
        if (t.kind() == QUESTION) {
            return conditionalExpr();
        } else {
            return logicalOrExpr();
        }
    }

    private Expr conditionalExpr() throws PLCCompilerException {
        IToken firstToken = t;
        if (t.kind() == QUESTION) {
            match(QUESTION);
            Expr guard = expr();
            match(RARROW);
            Expr trueExpr = expr();
            match(COMMA);
            Expr falseExpr = expr();
            return new ConditionalExpr(firstToken, guard, trueExpr, falseExpr);
        } else {
            return logicalOrExpr();
        }
    }

    private Expr logicalOrExpr() throws PLCCompilerException {
        IToken firstToken = t;
        Expr left = logicalAndExpr();
        while (Arrays.asList(BITOR, OR).contains(t.kind())) {
            IToken op = t;
            consume();
            Expr right = logicalAndExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    private Expr logicalAndExpr() throws PLCCompilerException {
        IToken firstToken = t;
        Expr left = comparisonExpr();
        while (Arrays.asList(BITAND, AND).contains(t.kind())) {
            IToken op = t;
            consume();
            Expr right = comparisonExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }


    private Expr comparisonExpr() throws PLCCompilerException {
        IToken firstToken = t;
        Expr left = powExpr();
        while (Arrays.asList(LT, GT, EQ, LE, GE).contains(t.kind())) {
            IToken op = t;
            consume();
            Expr right = powExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    private Expr powExpr() throws PLCCompilerException {
        IToken firstToken = t;
        Expr left = additiveExpr();
        if (t.kind() == EXP) {
            IToken op = t;
            consume();
            Expr right = powExpr();
            return new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    private Expr additiveExpr() throws PLCCompilerException {
        IToken firstToken = t;
        Expr left = multiplicativeExpr();
        while (Arrays.asList(PLUS, MINUS).contains(t.kind())) {
            IToken op = t;
            consume();
            Expr right = multiplicativeExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    private Expr multiplicativeExpr() throws PLCCompilerException {
        IToken firstToken = t;
        Expr left = unaryExpr();
        while (Arrays.asList(TIMES, DIV, MOD).contains(t.kind())) {
            IToken op = t;
            consume();
            Expr right = unaryExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    private Expr unaryExpr() throws PLCCompilerException {
        IToken firstToken = t;
        if (Arrays.asList(BANG, MINUS).contains(t.kind()) ||
                Arrays.asList(RES_width, RES_height).contains(t.kind())) {
            IToken op = t;
            consume();
            Expr expr = unaryExpr();
            return new UnaryExpr(firstToken, op, expr);
        } else {
            return postfixExpr();
        }
    }

    private Expr postfixExpr() throws PLCCompilerException {
	    IToken firstToken = t;

	    Expr primary = primaryExpr();

	    if (primary == null) {
	        return null;
	    }

	    PixelSelector pixelSelector = null;
	    ChannelSelector channelSelector = null;

	    if (t.kind() == LSQUARE) {
	        pixelSelector = pixelSelector();
	    }

	    if (t.kind() == COLON) {
	        consume();
	        if (Arrays.asList(RES_red, RES_green, RES_blue).contains(t.kind())) {
	            channelSelector = channelSelector();
	        }
	    }

	    if (pixelSelector != null || channelSelector != null) {
	        return new PostfixExpr(firstToken, primary, pixelSelector, channelSelector);
	    }

	    return primary;
	}


    private PixelSelector pixelSelector() throws PLCCompilerException {
        IToken firstToken = t;
        match(LSQUARE);
        Expr x = expr();
        match(COMMA);
        Expr y = expr();
        match(RSQUARE);
        PixelSelector pixelSelector = new PixelSelector(firstToken, x, y);
        return pixelSelector;
    }

    private ChannelSelector channelSelector() throws PLCCompilerException {
        IToken firstToken = t;
        switch (t.kind()) {
            case RES_red:
                consume();
                return new ChannelSelector(firstToken, firstToken);
            case RES_green:
                consume();
                return new ChannelSelector(firstToken, firstToken);
            case RES_blue:
                consume();
                return new ChannelSelector(firstToken, firstToken);
            default:
                throw new SyntaxException("Expected channel selector");
        }
    }

    
    private Expr primaryExpr() throws PLCCompilerException {
	    IToken firstToken = t;
	    switch (t.kind()) {
	        case STRING_LIT:
	            consume();
	            return new StringLitExpr(firstToken);
	        case NUM_LIT:
	            consume();
	            return new NumLitExpr(firstToken);
	        case IDENT:
	            consume();
	            if (t.kind() == LSQUARE) {
	                PixelSelector pixelSelector = pixelSelector();
	                if (t.kind() == COLON) {
	                    consume();
	                    ChannelSelector channelSelector = channelSelector();
	                    return new PostfixExpr(firstToken, new IdentExpr(firstToken), pixelSelector, channelSelector);
	                } else {
	                    return new PostfixExpr(firstToken, new IdentExpr(firstToken), pixelSelector, null);
	                }
	            } else if (t.kind() == COLON) {
	                consume();
	                ChannelSelector channelSelector = channelSelector();
	                return new PostfixExpr(firstToken, new IdentExpr(firstToken), null, channelSelector);
	            } else if (t.kind() == QUESTION) {
	                return conditionalExpr();
	            }
	            return new IdentExpr(firstToken);
	        case LPAREN:
	            consume();
	            Expr expr = expr();
	            match(RPAREN);
	            return expr;
	        case CONST:
	            consume();
	            return new ConstExpr(firstToken);
	        case BOOLEAN_LIT:
	            return booleanLitExpr();
	        case BANG:
	        case MINUS:
	        case RES_width:
	        case RES_height:
	            IToken op = t;
	            consume();
	            Expr unaryExpr = unaryExpr();
	            return new UnaryExpr(firstToken, op, unaryExpr);
	        default:
	            return expandedPixelExpr();
	    }
	}
    
    private Expr expandedPixelExpr() throws PLCCompilerException {
        IToken firstToken = t;
        match(LSQUARE);
        Expr r = expr();
        match(COMMA);
        Expr g = expr();
        match(COMMA);
        Expr b = expr();
        match(RSQUARE);
        return new ExpandedPixelExpr(firstToken, r, g, b);
    }

    private Expr booleanLitExpr() throws PLCCompilerException {
        IToken firstToken = t;
        if ("TRUE".equalsIgnoreCase(t.text()) || "FALSE".equalsIgnoreCase(t.text())) {
            consume();
            return new BooleanLitExpr(firstToken);
        } else {
            throw new SyntaxException("Expected 'TRUE' or 'FALSE', found: " + t.text());
        }
    }



    private Dimension dimension() throws PLCCompilerException {
        IToken firstToken = t;
        Expr widthExpr = null;
        Expr heightExpr = null;

        if (t.kind() == LSQUARE) {
            match(LSQUARE);
            widthExpr = expr();
            match(COMMA);
            heightExpr = expr();
            match(RSQUARE);
        }

        return new Dimension(firstToken, widthExpr, heightExpr);
    }

    private LValue lValue() throws PLCCompilerException {
        IToken firstToken = t;
        IToken nameToken = t;
        match(IDENT);

        PixelSelector pixelSelector = null;
        if (t.kind() == LSQUARE) {
            pixelSelector = pixelSelector();
        }

        ChannelSelector channelSelector = null;
        if (t.kind() == COLON) {
            consume();  
            if (Arrays.asList(RES_red, RES_green, RES_blue).contains(t.kind())) {
                channelSelector = channelSelector();
            } else {
                throw new SyntaxException("Expected channel selector after colon");
            }
        }

        return new LValue(firstToken, nameToken, pixelSelector, channelSelector);
    }


    private Statement statement() throws PLCCompilerException {
        IToken firstToken = t;
        switch (t.kind()) {
            case IDENT:
                LValue lValue = lValue();
                match(Kind.ASSIGN);
                Expr expr = expr();
                return new AssignmentStatement(firstToken, lValue, expr);

            case RES_write:
                match(Kind.RES_write);
                expr = expr();
                return new WriteStatement(firstToken, expr);

            case RES_do:
                match(Kind.RES_do);
                List<GuardedBlock> guardedBlocks = new ArrayList<>();
                do {
                    guardedBlocks.add(guardedBlock());
                } while (t.kind() == Kind.BOX);
                match(Kind.RES_od);
                return new DoStatement(firstToken, guardedBlocks);


            case RES_if: 
                match(Kind.RES_if);
                List<GuardedBlock> guardedBlocks4 = new ArrayList<>();
                do {
                    guardedBlocks4.add(guardedBlock());
                    if (t.kind() == Kind.BOX) {
                        consume(); 
                    } else {
                        break;
                    }
                } while (true);
                match(Kind.RES_fi); 
                return new IfStatement(firstToken, guardedBlocks4);

            case RETURN:
                match(Kind.RETURN);
                expr = expr();
                return new ReturnStatement(firstToken, expr);

            case BLOCK_OPEN:
                Block block = block();
                return new StatementBlock(firstToken, block);
                
            default:
                throw new PLCCompilerException("Unexpected token: " + t.kind());
        }
    }

    private GuardedBlock guardedBlock() throws PLCCompilerException {
        IToken firstToken = t;
        Expr guard;
        if (t.kind() == BOX) {
            consume(); 
            guard = expr();
        } else {
            guard = expr();
        }
        if (t.kind() == SEMI) {
            throw new SyntaxException("Unexpected semicolon after guard expression in 'do' statement");
        }

        match(Kind.RARROW);
        Block block = block();
        return new GuardedBlock(firstToken, guard, block);
    }

    private StatementBlock blockStatement() throws PLCCompilerException {
        IToken firstToken = t;
        Block block = block();
        return new StatementBlock(firstToken, block);
    }

    private void match(Kind kind) throws PLCCompilerException {
        if (t.kind() == kind) {
            consume();
        } else {
            throw new SyntaxException("Kind not found. Expected: " + kind + ", Found: " + t.kind());
        }
    }

    private void consume() throws PLCCompilerException {
        t = lexer.next();
    }
}
