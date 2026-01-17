package com.github.zomboiddecompiler.rosetta.vineflower;

import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.Pair;

import java.util.LinkedHashMap;
import java.util.Map;

/// Name provider for methods with no Rosetta data.
public class RosettaGenericNameProvider extends AbstractRosettaNameProvider {
    @Override
    public Map<VarVersionPair, String> rename(Map<VarVersionPair, Pair<VarType, String>> variables) {
        var localVars = method.getLocalVariableAttr();
        if (localVars != null) {
            return localVars.getMapNames();
        }

        Map<VarVersionPair, VarType> unknownVariables = new LinkedHashMap<>();
        for (var entry : variables.entrySet()) {
            // Skip null entries and use the variable type if available
            if (entry.getValue() != null && entry.getValue().a != null) {
                unknownVariables.put(entry.getKey(), entry.getValue().a);
            }
        }

        return assignUnknownVariableNames(unknownVariables, null);
    }

    public RosettaGenericNameProvider(StructMethod method) {
        this.method = method;
    }

    private final StructMethod method;
}
