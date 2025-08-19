package com.dev;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;

public class RemoveNeverUsedImports {

    public static void main(String[] args) throws IOException {
        // Define o diretório ou arquivo Java a ser processado
        // Alterar o caminho conforme necessário
        Path dir = Paths.get(
                "C:\\Users\\josiane\\Documents\\projeto-smart");

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
                // Anotações sem membros
                @Override
                public void visit(MarkerAnnotationExpr annotation, Void args) {
                    usedIdentifiers.add(annotation.getNameAsString());
                    super.visit(annotation, args);
                }

                // Anotações com um único membro
                @Override
                public void visit(SingleMemberAnnotationExpr annotation, Void args) {
                    usedIdentifiers.add(annotation.getNameAsString());
                    super.visit(annotation, args);
                }

                // Anotações com múltiplos membros
                @Override
                public void visit(NormalAnnotationExpr annotation, Void args) {
                    usedIdentifiers.add(annotation.getNameAsString());
                    super.visit(annotation, args);
                }

                // Tipos e declarações genericas 
                @Override
                public void visit(ClassOrInterfaceType type, Void args) {
                    usedIdentifiers.add(type.getNameAsString());
                    super.visit(type, args);
                }

                @Override
                public void visit(MethodCallExpr methodCall, Void args) {
                    usedIdentifiers.add(methodCall.getNameAsString());
                    super.visit(methodCall, args);
                }

                // Referência a tipos de retorno de métodos
                @Override
                public void visit(FieldDeclaration fieldAccess, Void args) {
                    fieldAccess.getElementType().ifClassOrInterfaceType(
                            type -> usedIdentifiers.add(type.getNameAsString()));
                    super.visit(fieldAccess, args);
                }

                // Referência a construtores e instâncias de classes
                @Override
                public void visit(ObjectCreationExpr objectCreationExpr, Void args) {
                    usedIdentifiers.add(objectCreationExpr.getType().getNameAsString());
                    super.visit(objectCreationExpr, args);
                }

                
                // Referência a classes, enums, constantes
                @Override
                public void visit(NameExpr nameExpr, Void args) {
                    usedIdentifiers.add(nameExpr.getNameAsString());
                    super.visit(nameExpr, args);
                }

                // Referência a variáveis, parâmetros, etc.
                @Override
                public void visit(MethodDeclaration method, Void args) {
                    usedIdentifiers.add(method.getNameAsString());
                    super.visit(method, args);
                }
            }, null);

            List<ImportDeclaration> notUsedImport = imports.stream()
    .filter(impt -> {
        String importName = impt.getName().getIdentifier();

        // Verifica se o import é um wildcard 
        if (impt.isAsterisk()) {
            String packageName = impt.getName().asString().replace(".*", "");
            return usedIdentifiers.stream()
                .noneMatch(id -> {
                    System.out.println("Verificando se " + id + " começa com " + packageName);
                    return true; 
                });
        }

        return !usedIdentifiers.contains(importName);
    })
    .collect(Collectors.toList());

            // Remover imports não utilizados
            if (!notUsedImport.isEmpty()) {
                System.out.println("Arquivo " + path + " possui imports não utilizados.");
                System.out.println("Imports removidos:");
                for (ImportDeclaration imp : notUsedImport) {
                    System.out.println(" - " + imp.getNameAsString());
                    cn.remove(imp);
                }
                System.out.println("---------------------------");
            }
            Files.write(path, cn.toString().getBytes());

        } catch ( IOException e ) {
            System.out.println("Erro: " + e);
            e.printStackTrace();
        }
    }
}