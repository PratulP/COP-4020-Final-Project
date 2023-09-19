package edu.ufl.cise.cop4020fa23;

import static edu.ufl.cise.cop4020fa23.Kind.EOF;

import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import java.util.HashMap;
import java.util.Map;

public class Lexer implements ILexer {
    private char[] chars;
    private int pos;
    private int startPos;
    private State state;
    private int line;
    private int column;
    private Map<String, Kind> reservedWordsMap = new HashMap<>();

    public Lexer(String input) {
        this.chars = (input + "\0").toCharArray(); 
        this.pos = 0;
        this.startPos = 0;
        this.state = State.START;
        this.line = 1;  
        this.column = 0;
        initializeReservedWords();
    }

    private void initializeReservedWords() {
        reservedWordsMap.put("image", Kind.RES_image);
        reservedWordsMap.put("pixel", Kind.RES_pixel);
        reservedWordsMap.put("int", Kind.RES_int);
        reservedWordsMap.put("string", Kind.RES_string);
        reservedWordsMap.put("void", Kind.RES_void);
        reservedWordsMap.put("boolean", Kind.RES_boolean);
        reservedWordsMap.put("write", Kind.RES_write);
        reservedWordsMap.put("height", Kind.RES_height);
        reservedWordsMap.put("width", Kind.RES_width);
        reservedWordsMap.put("if", Kind.RES_if);
        reservedWordsMap.put("fi", Kind.RES_fi);
        reservedWordsMap.put("do", Kind.RES_do);
        reservedWordsMap.put("od", Kind.RES_od);
        reservedWordsMap.put("red", Kind.RES_red);
        reservedWordsMap.put("green", Kind.RES_green);
        reservedWordsMap.put("blue", Kind.RES_blue);
        reservedWordsMap.put("Z", Kind.CONST);
        reservedWordsMap.put("BLACK", Kind.CONST);
        reservedWordsMap.put("BLUE", Kind.CONST);
        reservedWordsMap.put("CYAN", Kind.CONST);
        reservedWordsMap.put("DARK_GRAY", Kind.CONST);
        reservedWordsMap.put("GRAY", Kind.CONST);
        reservedWordsMap.put("GREEN", Kind.CONST);
        reservedWordsMap.put("LIGHT_GRAY", Kind.CONST);
        reservedWordsMap.put("MAGENTA", Kind.CONST);
        reservedWordsMap.put("ORANGE", Kind.CONST);
        reservedWordsMap.put("PINK", Kind.CONST);
        reservedWordsMap.put("RED", Kind.CONST);
        reservedWordsMap.put("WHITE", Kind.CONST);
        reservedWordsMap.put("YELLOW", Kind.CONST);
        reservedWordsMap.put("TRUE", Kind.BOOLEAN_LIT);
        reservedWordsMap.put("FALSE", Kind.BOOLEAN_LIT);
    }


    @Override
    public IToken next() throws LexicalException {
        while (true) {
            char ch = chars[pos];
            column++;  

            switch (state) {
                case START:
                    startPos = pos;
                    if (ch == '"') {
                        pos++;

                        while (chars[pos] != '"' && chars[pos] != '\0' && chars[pos] != '\n') {
                            pos++;
                        }
                        
                        if (chars[pos] == '"') {
                            pos++;
                            return new Token(Kind.STRING_LIT, startPos, pos - startPos, chars, new SourceLocation(line, column));
                        } else if (chars[pos] == '\n') {
                            throw new LexicalException(new SourceLocation(line, column), "Unterminated string literal with newline");
                        } else {
                            throw new LexicalException(new SourceLocation(line, column), "Unterminated string literal");
                        }
                    }

                    if (Character.isJavaIdentifierStart(ch)) {  
                        state = State.IN_IDENT;
                        pos++;
                        continue;  
                    }
                    if (Character.isDigit(ch)) {
                        if (ch == '0') {
                            state = State.HAVE_ZERO;
                        } else {
                            state = State.IN_NUM;
                        }
                        pos++;
                        continue;
                    }
                    switch (ch) 
                    {
                    	case ' ':
                    	case '\t':
                        pos++;
                        break;
                    	case '\n':
                        pos++;
                        line++;
                        column = 0;
                        break;
                    	case '\r':
                        pos++;
                        break;
                        case '\0':  
                            return new Token(EOF, pos, 1, chars, new SourceLocation(line, column));
                        case '+':
                            pos++;
                            return new Token(Kind.PLUS, startPos, 1, chars, new SourceLocation(line, column));
                        case '=':
                            if (chars[pos + 1] == '=') 
                            {
                                pos += 2;
                                return new Token(Kind.EQ, startPos, 2, chars, new SourceLocation(line, column));
                            } else {
                                pos++;
                                return new Token(Kind.ASSIGN, startPos, 1, chars, new SourceLocation(line, column));
                            }

                        case '0':
                            state = State.HAVE_ZERO;
                            pos++;
                            break;
                        case ',':
                            pos++;
                            return new Token(Kind.COMMA, startPos, 1, chars, new SourceLocation(line, column));
                        case '<':
                            if (chars[pos + 1] == ':') {
                                pos += 2;
                                return new Token(Kind.BLOCK_OPEN, startPos, 2, chars, new SourceLocation(line, column));
                            } else if (chars[pos + 1] == '=') {
                                pos += 2;
                                return new Token(Kind.LE, startPos, 2, chars, new SourceLocation(line, column));
                            } else {
                                pos++;
                                return new Token(Kind.LT, startPos, 1, chars, new SourceLocation(line, column));
                            }
                        case ':':
                            if (chars[pos + 1] == '>') {
                                pos += 2;
                                return new Token(Kind.BLOCK_CLOSE, startPos, 2, chars, new SourceLocation(line, column));
                            } else {
                                pos++;
                                return new Token(Kind.COLON, startPos, 1, chars, new SourceLocation(line, column));
                            }
                            
                        case '-':
                            if (chars[pos + 1] == '>') {
                                pos += 2;  
                                return new Token(Kind.RARROW, startPos, 2, chars, new SourceLocation(line, column));
                            } else {
                                pos++;
                                return new Token(Kind.MINUS, startPos, 1, chars, new SourceLocation(line, column));
                            }
                        case '>': 
                            pos++;
                            return new Token(Kind.GT, startPos, 1, chars, new SourceLocation(line, column));
                        case '*':
                            pos++;
                            return new Token(Kind.TIMES, startPos, 1, chars, new SourceLocation(line, column));

                        case '[':
                            if (chars[pos + 1] == ']') {
                                pos += 2;
                                return new Token(Kind.BOX, startPos, 2, chars, new SourceLocation(line, column));
                            } else {
                                pos++;
                                return new Token(Kind.LSQUARE, startPos, 1, chars, new SourceLocation(line, column));
                            }
                        case ']':
                            pos++;
                            return new Token(Kind.RSQUARE, startPos, 1, chars, new SourceLocation(line, column));
                        case '%':
                            pos++;
                            return new Token(Kind.MOD, startPos, 1, chars, new SourceLocation(line, column));
                        case '/':
                            pos++;
                            return new Token(Kind.DIV, startPos, 1, chars, new SourceLocation(line, column));
                        case '?':
                            pos++;
                            return new Token(Kind.QUESTION, startPos, 1, chars, new SourceLocation(line, column));
                        case '!':
                            pos++;
                            return new Token(Kind.BANG, startPos, 1, chars, new SourceLocation(line, column));
                        case ';':
                            pos++;
                            return new Token(Kind.SEMI, startPos, 1, chars, new SourceLocation(line, column));
                        case '#':
                            pos++;  
                            if (chars[pos] == '#') {  
                                pos++;  
                                while (chars[pos] != '\n' && chars[pos] != '\0') {
                                    pos++; 
                                }
                                if (chars[pos] == '\n') {
                                    line++;
                                    column = 0;
                                    pos++;
                                }
                                break;
                            } else {
                                throw new LexicalException(new SourceLocation(line, column), "Single '#' is not a valid token. Expected '##' for comment.");
                            }
                        case '&':
                            pos++; 
                            if (chars[pos] == '&') {  
                                pos++; 
                                column++;  
                                return new Token(Kind.AND, startPos, 2, chars, new SourceLocation(line, column - 1));
                            } else {
                                return new Token(Kind.BITAND, startPos, 1, chars, new SourceLocation(line, column));
                            }
                        default:
                            throw new LexicalException(new SourceLocation(line, column), "Unexpected character: " + ch);
                    }
                    break;
                case HAVE_EQ:
                    if (ch == '=') {
                        pos++;
                        state = State.START;
                        return new Token(Kind.EQ, startPos, 2, chars, new SourceLocation(line, column));
                    } else {
                        throw new LexicalException(new SourceLocation(line, column), "Unexpected character: " + ch);
                    }
                    
                case HAVE_ZERO:
                    state = State.START;
                    return new Token(Kind.NUM_LIT, startPos, 1, chars, new SourceLocation(line, column));
                    
                case IN_NUM:
                    while (Character.isDigit(chars[pos])) {
						pos++;
					}

					String chArray = String.copyValueOf(chars);
					String temp = chArray.substring(startPos,pos);

					try {
						int num = Integer.parseInt(temp);
						if (num < Integer.MAX_VALUE) {
							state = State.START;
							return new Token(Kind.NUM_LIT, startPos, pos - startPos, chars, new SourceLocation(line, column));
						} else {
							throw new LexicalException("Number is out of range");
						}
					} catch (NumberFormatException e) {
						throw new LexicalException("Invalid");
					}

                case IN_IDENT:
                	while (Character.isJavaIdentifierPart(chars[pos])) 
                	{
                        pos++;
                    }
                    String ident = new String(chars, startPos, pos - startPos);  

                    Kind kind = reservedWordsMap.get(ident);
                    if (kind == null) {  
                        state = State.START;
                        return new Token(Kind.IDENT, startPos, ident.length(), chars, new SourceLocation(line, column));
                    } else {  
                        state = State.START;
                        return new Token(kind, startPos, ident.length(), chars, new SourceLocation(line, column));
                    }
                default:
                    throw new LexicalException(new SourceLocation(line, column), "Unexpected character: " + ch);
            }
        }
    }



    private enum State {
        START, IN_IDENT, HAVE_ZERO, HAVE_EQ, HAVE_DOT, IN_FLOAT, IN_NUM, HAVE_MINUS
    }
}
