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
        IToken firstToken = t;
        IToken typeToken = t;
        match(Kind.RES_void);
        IToken nameToken = t;
        match(IDENT);
        match(LPAREN);
        ArrayList<NameDef> paramList = param_list();
        match(RPAREN);

       // Block block = block();

        Block block = null;


       if (t.kind() == BLOCK_OPEN) {
           block = block();
        }

        return new Program(firstToken, typeToken, nameToken, paramList, block);
    }



    private Block block() throws PLCCompilerException {
        IToken firstToken = t;
        List<Block.BlockElem> elems = new ArrayList<>();


       match(BLOCK_OPEN);


        while (Arrays.asList(RES_int, RES_string, RES_boolean).contains(t.kind())) {
            elems.add(declaration());
        }

        while (t.kind() != RARROW && t.kind() != EOF && t.kind() != BLOCK_CLOSE) {
            elems.add(statement());
        }


        match(BLOCK_CLOSE);

        return new Block(firstToken, elems);
    }



    private ArrayList<NameDef> param_list() throws PLCCompilerException {
        ArrayList<NameDef> params = new ArrayList<>();
        IToken firstToken = t;
        if (t.kind() == RPAREN) {

            return params;
        }

        //same previous implementation but trying if-else statement instead

        /*else {
            IToken typeToken = t;
            Type type = type();
            IToken identToken = t;
            match(IDENT);

            Dimension dimension = null;

            if (t.kind() == LSQUARE) {
                dimension = dimension();
            }

            if (dimension != null) {
                params.add(new NameDef(identToken, typeToken, dimension, identToken));
            } else {
                params.add(new NameDef(identToken, typeToken, null, identToken));
            }

            // if (t.kind() != COMMA) {
            //  break;
            // }

            match(COMMA);
        } */

       while (true) {
            IToken typeToken = t;
            Type type = type();
            IToken identToken = t;
            match(IDENT);

            Dimension dimension = null;

            if (t.kind() == LSQUARE) {
                dimension = dimension();
            }

            if (dimension != null) {
                params.add(new NameDef(identToken, typeToken, dimension, identToken));
            } else {
                params.add(new NameDef(identToken, typeToken, null, identToken));
            }

            if (t.kind() != COMMA) {
                break;
            }

            match(COMMA);
        }

        //different approach #1

       /* NameDef nameDef = nameDef();
        params.add(nameDef);
        while (t.kind() == COMMA) {
            match(COMMA);
            nameDef = nameDef();
            params.add(nameDef);
        }*/


        //different approach #2

        /*if (t.kind() == RES_image || t.kind() == RES_pixel || t.kind() == RES_int || t.kind() == RES_string || t.kind() == RES_void) {
            NameDef nameDef = nameDef();
            params.add(nameDef);

            while (t.kind() == COMMA) {
                match(COMMA);
                NameDef next = nameDef();
                params.add(next);
            }
        }*/



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

        IToken identToken = t;
        match(IDENT);

        if (dimension != null) {

            return new NameDef(firstToken, typeToken, dimension, identToken);
        } else {
            return new NameDef(firstToken, typeToken, null, identToken);
        }
    }




    private Type type() throws PLCCompilerException {
        IToken firstToken = t;
        //added the matches to each case (not sure if correct)
        switch (t.kind()) {
            case RES_image:
                match(RES_image);
            case RES_pixel:
                match(RES_pixel);
            case RES_int:
                match(RES_int);
            case RES_string:
                match(RES_string);
            case RES_boolean:
                match(RES_boolean);
               // consume();

            default:
                throw new SyntaxException("Expected a valid type");
        }
    }

    private Declaration declaration() throws PLCCompilerException {
        IToken firstToken = t;
        Expr e;
        Type type = type();
        NameDef nameDef = nameDef();
        if (t.kind() == ASSIGN) {
            match(ASSIGN);
            e = expr();
        } else {
            e = null;
        }
        //match(SEMI);
        return new Declaration(firstToken, nameDef, e);
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
        IdentExpr ident = new IdentExpr(t);
        match(IDENT);

        if (t.kind() == LSQUARE) {
            PixelSelector pixelSelector = pixelSelector();
            if (t.kind() == COLON) {
                match(COLON);
                ChannelSelector channelSelector = channelSelector();
                return new LValue(firstToken, ident.firstToken, pixelSelector, channelSelector);
            } else {
                return new LValue(firstToken, ident.firstToken, pixelSelector, null);
            }
        } else if (t.kind() == COLON) {
            match(COLON);
            ChannelSelector channelSelector = channelSelector();
            return new LValue(firstToken, ident.firstToken, null, channelSelector);
        } else if (t.kind() == QUESTION) {

            return new LValue(firstToken, ident.firstToken, null, null);
        }

        return new LValue(firstToken, ident.firstToken, null, null);
    }



    private Statement statement() throws PLCCompilerException {
        switch (t.kind()) {
            case RES_if:
                //      return ifStatement();
                //   case RES_while:
                //       return whileStatement();
            case RES_do:
                //       return doStatement();
                //    case RES_for:
                //      return forStatement();
            case BLOCK_OPEN: // Assuming BLOCK_OPEN corresponds to '<:' (Block open)
                return blockStatement();
            case IDENT:
                //      return assignmentOrCallStatement();
                //  case RES_return:
                //     return returnStatement();
            case RES_write:
                // return writeStatement();
            default:
                throw new SyntaxException("Invalid statement");
        }
    }



    private GuardedBlock guardedBlock() throws PLCCompilerException {
        IToken firstToken = t;

        match(Kind.RES_if);

        Expr guard = expr();

        match(Kind.RES_fi);

        Block block = block();

        return new GuardedBlock(firstToken, guard, block);
    }


    private StatementBlock blockStatement() throws PLCCompilerException {
        IToken firstToken = t;
        match(Kind.BLOCK_OPEN);

        Block block = block();

        match(Kind.BLOCK_CLOSE);

        return new StatementBlock(firstToken, block);
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

    private void consume() throws PLCCompilerException {
        t = lexer.next();
    }
}
