package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.AST;
import edu.ufl.cise.cop4020fa23.ast.NameDef;

import java.util.HashMap;

public class SymbolTable {

    //enterScope, leaveScope, insert(NameDef), lookup(name)

    HashMap<String, NameDef> table = new HashMap<>();
    // HashMap<String, Declaration> table = new HashMap<>();

    void enterScope() {}

    void leaveScope() {}

    void insert(NameDef namedef) {}

    void lookup(String name) {}
}
