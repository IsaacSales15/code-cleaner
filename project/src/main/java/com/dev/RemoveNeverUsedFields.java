package com.dev;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;

public class RemoveNeverUsedFields {
    public static void main(String[] args) throws IOException {
        Path dir = Paths.get("");

        if (Files.isDirectory(dir)) {
            Files.walk(dir).filter(path -> path.toString().endsWith(".java"))
                    .forEach(RemoveNeverUsedFields::processFiles);
        } else if (dir.toString().endsWith(".java")) {
            processFiles(dir);
        } else {
            System.out.println("O diretório não é um arquivo compatível");
        }
    }

    private static void processFiles(Path path) {
        try {
            CompilationUnit cn = StaticJavaParser.parse(path);
            List<ImportDeclaration> fields = cn.getImports();

            Set<String> usedIdentifiers = new HashSet<>();
            cn.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(FieldDeclaration declaration, Void args) {
                    for (VariableDeclarator var : declaration.getVariables()) {
                        String varName = var.getNameAsString();

                        if (!usedIdentifiers.contains(varName)) {
                            usedIdentifiers.add(varName);
                        }
                    }
                }

                @Override
                public void visit(FieldAccessExpr accessExpr, Void args) {
                    usedIdentifiers.add(accessExpr.getNameAsString());
                    super.visit(accessExpr, args);
                }

                @Override
                public void visit(NameExpr nameExpr, Void args) {
                    usedIdentifiers.add(nameExpr.getNameAsString());
                    super.visit(nameExpr, args);
                }
            }, null);

            List<ImportDeclaration> notUsedFields = fields.stream()
                    .filter(field -> {
                        String fieldName = field.getName().getIdentifier();
                    }).collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
