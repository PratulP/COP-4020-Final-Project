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

import static edu.ufl.cise.cop4020fa23.Kind.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private AST program() throws PLCCompilerException {
        //throw new UnsupportedOperationException();
        IToken firstToken = t;
        Type type = new Type(); //needs implementation
        IdentExpr ident;
        match(LPAREN);
        ArrayList<NameDef> ParamList = new param_list(); //needs implementation
        match(RPAREN);
        Block block; //needs implementation
        return new Program(firstToken, type, ident, ParamList, block);
    }

    private Block block() throws PLCCompilerException {
        IToken firstToken = t;
        //match();
        // new declaration
        // new statement
        return new Block(firstToken, declaration, statement);
    }

    private ArrayList<NameDef> param_list() throws PLCCompilerException {

    }

    private NameDef nameDef() throws PLCCompilerException {

    }

    private Type type() throws PLCCompilerException {


    }

    private Declaration declaration() throws PLCCompilerException {

    }


     // REST OF GRAMMAR AFTER HW1



    //begins HW1

    private Expr expr() throws PLCCompilerException {
        //IToken firstToken = t;
        //throw new UnsupportedOperationException("THE PARSER HAS NOT BEEN IMPLEMENTED YET");
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

        if (Arrays.asList(RES_red, RES_green, RES_blue).contains(t.kind())) {
            channelSelector = channelSelector();
        }

        if (t.kind() == LSQUARE) {
            pixelSelector = pixelSelector();
        }

        if (pixelSelector == null && channelSelector == null) {
            return primary;
        }

        return new PostfixExpr(firstToken, primary, pixelSelector, channelSelector);
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

    private Expr booleanLitExpr() throws PLCCompilerException {
        IToken firstToken = t;
        if (t.text().equals("TRUE") || t.text().equals("FALSE")) {
            boolean isTrue = t.text().equals("TRUE");
            consume();
            return new BooleanLitExpr(firstToken);
        } else {
            throw new SyntaxException("Expected 'TRUE' or 'FALSE'");
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

    //continues HW2

    private Dimension dimension() throws PLCCompilerException {

    }

    private LValue lValue() throws PLCCompilerException {

    }

    private Statement statement() throws PLCCompilerException {}

    private GuardedBlock guardedBlock() throws PLCCompilerException {}

    private StatementBlock blockStatement() throws PLCCompilerException {}




    private void match(Kind kind) throws PLCCompilerException {
        if (t.kind() == kind) {
            consume();
        } else {
            throw new SyntaxException("Kind not found");
        }
    }

    private void consume() throws PLCCompilerException {
        t = lexer.next();
    }
}




}