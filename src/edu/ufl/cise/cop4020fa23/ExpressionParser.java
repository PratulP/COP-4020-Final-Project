/* Copyright 2023 by Beverly A Sanders
 *
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the
 * University of Florida during the fall semester 2023 as part of the course project.
 *
 * No other use is authorized.
 *
 * This code may not be posted on a public web site either during or after the course.
 */

package edu.ufl.cise.cop4020fa23;

import static edu.ufl.cise.cop4020fa23.Kind.AND;
import static edu.ufl.cise.cop4020fa23.Kind.BANG;
import static edu.ufl.cise.cop4020fa23.Kind.BOOLEAN_LIT;
import static edu.ufl.cise.cop4020fa23.Kind.BITAND;
import static edu.ufl.cise.cop4020fa23.Kind.BITOR;
import static edu.ufl.cise.cop4020fa23.Kind.COLON;
import static edu.ufl.cise.cop4020fa23.Kind.COMMA;
import static edu.ufl.cise.cop4020fa23.Kind.DIV;
import static edu.ufl.cise.cop4020fa23.Kind.EOF;
import static edu.ufl.cise.cop4020fa23.Kind.EQ;
import static edu.ufl.cise.cop4020fa23.Kind.EXP;
import static edu.ufl.cise.cop4020fa23.Kind.GE;
import static edu.ufl.cise.cop4020fa23.Kind.GT;
import static edu.ufl.cise.cop4020fa23.Kind.IDENT;
import static edu.ufl.cise.cop4020fa23.Kind.LE;
import static edu.ufl.cise.cop4020fa23.Kind.LPAREN;
import static edu.ufl.cise.cop4020fa23.Kind.LSQUARE;
import static edu.ufl.cise.cop4020fa23.Kind.LT;
import static edu.ufl.cise.cop4020fa23.Kind.MINUS;
import static edu.ufl.cise.cop4020fa23.Kind.MOD;
import static edu.ufl.cise.cop4020fa23.Kind.NUM_LIT;
import static edu.ufl.cise.cop4020fa23.Kind.OR;
import static edu.ufl.cise.cop4020fa23.Kind.PLUS;
import static edu.ufl.cise.cop4020fa23.Kind.QUESTION;
import static edu.ufl.cise.cop4020fa23.Kind.RARROW;
import static edu.ufl.cise.cop4020fa23.Kind.RES_blue;
import static edu.ufl.cise.cop4020fa23.Kind.RES_green;
import static edu.ufl.cise.cop4020fa23.Kind.RES_height;
import static edu.ufl.cise.cop4020fa23.Kind.RES_red;
import static edu.ufl.cise.cop4020fa23.Kind.RES_width;
import static edu.ufl.cise.cop4020fa23.Kind.RPAREN;
import static edu.ufl.cise.cop4020fa23.Kind.RSQUARE;
import static edu.ufl.cise.cop4020fa23.Kind.STRING_LIT;
import static edu.ufl.cise.cop4020fa23.Kind.TIMES;
import static edu.ufl.cise.cop4020fa23.Kind.CONST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.ufl.cise.cop4020fa23.ast.AST;
import edu.ufl.cise.cop4020fa23.ast.BinaryExpr;
import edu.ufl.cise.cop4020fa23.ast.BooleanLitExpr;
import edu.ufl.cise.cop4020fa23.ast.ChannelSelector;
import edu.ufl.cise.cop4020fa23.ast.ConditionalExpr;
import edu.ufl.cise.cop4020fa23.ast.ConstExpr;
import edu.ufl.cise.cop4020fa23.ast.ExpandedPixelExpr;
import edu.ufl.cise.cop4020fa23.ast.Expr;
import edu.ufl.cise.cop4020fa23.ast.IdentExpr;
import edu.ufl.cise.cop4020fa23.ast.NumLitExpr;
import edu.ufl.cise.cop4020fa23.ast.PixelSelector;
import edu.ufl.cise.cop4020fa23.ast.PostfixExpr;
import edu.ufl.cise.cop4020fa23.ast.StringLitExpr;
import edu.ufl.cise.cop4020fa23.ast.UnaryExpr;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;
import javax.swing.text.html.parser.Parser;

/**
 * Expr ::= ConditionalExpr | LogicalOrExpr
 * ConditionalExpr ::= ? Expr : Expr : Expr
 * LogicalOrExpr ::= LogicalAndExpr ( ( | | || ) LogicalAndExpr)*
 * LogicalAndExpr ::= ComparisonExpr ( ( & | && ) ComparisonExpr)*
 * ComparisonExpr ::= PowExpr ( (< | > | == | <= | >=) PowExpr)*
 * PowExpr ::= AdditiveExpr ** PowExpr | AdditiveExpr
 * AdditiveExpr ::= MultiplicativeExpr ( ( + | - ) MultiplicativeExpr )*
 * MultiplicativeExpr ::= UnaryExpr (( * | / | % ) UnaryExpr)*
 * UnaryExpr ::= ( ! | - | length | width) UnaryExpr | UnaryExprPostfix
 * UnaryExprPostfix ::= PrimaryExpr (PixelSelector | ε ) (ChannelSelector | ε )
 * PrimaryExpr ::= STRING_LIT | NUM_LIT | IDENT | ( Expr ) | Z ExpandedPixel
 * ChannelSelector ::= : red | : green | : blue
 * PixelSelector ::= [ Expr , Expr ]
 * ExpandedPixel ::= [ Expr , Expr , Expr ]
 * Dimension ::= [ Expr , Expr ]
 */
public class ExpressionParser implements IParser {
    
    final ILexer lexer;
    private IToken t;
    private int current = 0;
    private ArrayList<IToken> tokens = new ArrayList<IToken>();

    public ExpressionParser(ILexer lexer) throws LexicalException {
        super();
        this.lexer = lexer;
        t = lexer.next();
    }
    
    @Override
    public AST parse() throws PLCCompilerException {
        Expr e = expr();
        return e;
    }
    
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















































