package com.example.painel.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.example.painel");

    @Test
    @DisplayName("Controllers devem estar no pacote 'controllers'")
    void controllersShouldResideInPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..controllers..");
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Services devem estar no pacote 'services'")
    void servicesShouldResideInPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Service")
                .should().resideInAPackage("..services..");
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Repositories devem estar no pacote 'repository'")
    void repositoriesShouldResideInPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Repository")
                .should().resideInAPackage("..repository..");
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Controllers não devem acessar Repositories diretamente")
    void controllersShouldNotAccessRepositories() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controllers..")
                .should().dependOnClassesThat().resideInAPackage("..repository..");
        rule.check(importedClasses);
    }
}
