package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
import java.util.HashMap;
import java.util.Stack;

public class SymbolTable {
    private int currentScope;
    private int nextScope;
    private Stack<Integer> scopeStack;
    private HashMap<String, Entry> symbolTable;

    public class Entry {
        NameDef nameDef;
        int scope;
        Entry nextEntry;

        Entry(NameDef nameDef, int scope, Entry nextEntry) {
            this.nameDef = nameDef;
            this.scope = scope;
            this.nextEntry = nextEntry;
        }

        public int getScope() {
            return scope;
        }
    }

    public SymbolTable() {
        currentScope = 0;
        nextScope = 1; 
        scopeStack = new Stack<>();
        symbolTable = new HashMap<>();
        enterScope(); 
    }

    public void enterScope() {
        currentScope = nextScope++;
        scopeStack.push(currentScope);
        System.out.println("Entering scope: " + currentScope);
    }

    public void leaveScope() {
        if (!scopeStack.isEmpty()) {
            System.out.println("Leaving scope: " + scopeStack.peek());
            scopeStack.pop();
            if (!scopeStack.isEmpty()) {
                currentScope = scopeStack.peek();
                System.out.println("Returning to scope: " + currentScope);
            }
        }
    }


    public boolean insert(NameDef nameDef) {
        String ident = nameDef.getName();
        Entry existingEntry = symbolTable.get(ident);

        if (existingEntry != null && existingEntry.scope == currentScope) {
            return false;
        }

        Entry newEntry = new Entry(nameDef, currentScope, existingEntry);
        symbolTable.put(ident, newEntry);
        return true;
    }

    public NameDef lookup(String ident) {
        Entry entry = symbolTable.get(ident);
        while (entry != null) {
            if (scopeStack.contains(entry.scope)) {
                return entry.nameDef;
            }
            entry = entry.nextEntry;
        }
        return null; 
    }

    public int getCurrentScope() {
        return currentScope;
    }

    public boolean lookupLocal(String ident) {
        Entry entry = symbolTable.get(ident);
        while (entry != null) {
            if (entry.scope == currentScope) {
                return true; 
            }
            entry = entry.nextEntry;
        }
        return false; 
    }
}
