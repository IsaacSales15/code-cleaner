package com.dev;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;

import java.util.Set;

public class RemoveNeverUsedFields {
    public static void main(String[] args) throws IOException {
        Path dir = Paths.get("#");

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

            Set<String> declaredFields = new HashSet<>();
            Set<String> usedIdentifiers = new HashSet<>();
            cn.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(FieldDeclaration declaration, Void args) {
                    for (VariableDeclarator var : declaration.getVariables()) {
                        declaredFields.add(var.getNameAsString());
                    }
                    super.visit(declaration, args);
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

            Set<String> notUseFields = new HashSet<>(declaredFields);
            notUseFields.removeAll(usedIdentifiers);

            cn.findAll(FieldDeclaration.class).removeIf(fd -> {
                for (VariableDeclarator var : fd.getVariables()) {
                    if (notUseFields.contains(var.getNameAsString())) {
                        return true;
                    }
                }
                return false;
            });

            cn.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
                methodDeclaration.getBody().ifPresent(body -> {
                    Set<String> declaredLocals = new HashSet<>();
                    Set<String> usedLocals = new HashSet<>();

                    body.findAll(VariableDeclarator.class).forEach(variableDeclarator -> {
                        if (variableDeclarator.getParentNode().filter(p -> p instanceof VariableDeclarationExpr)
                                .isPresent()) {
                                    declaredLocals.add(variableDeclarator.getNameAsString());
                        }
                    });
                    body.findAll(NameExpr.class).forEach(nameExpr -> {
                        usedLocals.add(nameExpr.getNameAsString());
                    });
                });
            });

            Files.write(path, cn.toString().getBytes());

            System.out.println("Arquivo processado: " + path);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}