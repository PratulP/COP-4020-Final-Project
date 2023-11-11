package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.AST;
import edu.ufl.cise.cop4020fa23.ast.NameDef;

import java.util.HashMap;
import java.util.Stack;

public class SymbolTable {

    //enterScope, leaveScope, insert(NameDef), lookup(name)

    HashMap<String, NameDef> table = new HashMap<>();
    // HashMap<String, Declaration> table = new HashMap<>();

    Stack<HashMap<String, NameDef>> scope_stack;
    //Stack<Integer> scope_stack;

    int current_num;
    int next_num;
    void enterScope() {
        //current_num = next_num++;
        //scope_stack.push(current_num);
        scope_stack.push(table);
    }

    void leaveScope() {
        scope_stack.pop();
        //current_num = scope_stack.pop();
    }

    /*void insert (NameDef namedef) {
    }*/

    void insert(String name, NameDef namedef) {
        if (!scope_stack.isEmpty()) {
            scope_stack.peek().put(name, namedef);
        }
    }

    void lookup(String name) {}
}
