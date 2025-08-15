package com.dev;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.expr.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;

public class RemoveNeverUsedImports {
    public static void main(String[] args) throws IOException {
        Path dir = Paths.get(
                "#");

        if (Files.isDirectory(dir)) {
            Files.walk(dir).filter(path -> path.toString().endsWith(".java"))
                    .forEach(RemoveNeverUsedImports::processFiles);
        } else if (dir.toString().endsWith(".java")) {
            processFiles(dir);
        } else {
            System.out.println("O caminho fornecido não é um diretório ou arquivo Java válido.");
        }
    }

    private static void processFiles(Path path) {
        try {
            CompilationUnit cn = StaticJavaParser.parse(path);
            List<ImportDeclaration> imports = cn.getImports();

            Set<String> usedIdentifiers = new HashSet<>();
            cn.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MarkerAnnotationExpr annotation, Void args) {
                    usedIdentifiers.add(annotation.getNameAsString());
                    super.visit(annotation, args);
                }

                @Override
                public void visit(SingleMemberAnnotationExpr annotation, Void args) {
                    usedIdentifiers.add(annotation.getNameAsString());
                    super.visit(annotation, args);
                }

                @Override
                public void visit(NormalAnnotationExpr annotation, Void args) {
                    usedIdentifiers.add(annotation.getNameAsString());
                    super.visit(annotation, args);
                }
            }, null);

            List<ImportDeclaration> notUsedImport = imports.stream()
                    .filter(impt -> {
                        String importName = impt.getName().getIdentifier();
                        return !usedIdentifiers.contains(importName);
                    })
                    .collect(Collectors.toList());

            if (!notUsedImport.isEmpty()) {
                System.out.println("Arquivo: " + path);
                System.out.println("Imports removidos:");
                for (ImportDeclaration imp : notUsedImport) {
                    System.out.println(" - " + imp.getNameAsString());
                    cn.remove(imp);
                }
                System.out.println("---------------------------");
            }
            Files.write(path, cn.toString().getBytes());

        }catch(

    IOException e)
    {
        System.out.println("Erro: " + e);
        e.printStackTrace();
    }
}}