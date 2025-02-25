package me.fup.joy.proxygen.compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import me.fup.joy.proxygen.core.ProxyGen
import kotlin.sequences.forEach

class ComponentPreviewSymbolProcessor(
    private val logger: KSPLogger,
    private val generator: CodeGenerator,
) : SymbolProcessor {
    private val visitor = FindFunctionsVisitor()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getAllFiles().forEach { it.accept(visitor, Unit) }

        val unableToProcess = resolver.getAllFiles().filterNot { it.validate() }.toList()
        return unableToProcess
    }

    override fun finish() {
        generator.write()
        super.finish()
    }

    @OptIn(KspExperimental::class)
    inner class FindFunctionsVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            if (classDeclaration.isAnnotationPresent(ProxyGen::class)) {
                generator.add(classDeclaration)
            }
        }

        @OptIn(KspExperimental::class)
        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            function.declarations.forEach { it.accept(this, Unit) }
        }

        override fun visitFile(file: KSFile, data: Unit) {
            file.declarations.forEach { it.accept(this, Unit) }
        }
    }
}