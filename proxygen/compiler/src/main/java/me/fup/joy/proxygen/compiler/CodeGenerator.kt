package me.fup.joy.proxygen.compiler

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.Unit

private const val GENERATED_CLASS_SUFFIX = "ProxyGen"
private const val NOT_IMPLEMENTED_CODE_BLOCK = "TODO(\"Not yet implemented\")"
private const val DELEGATE_SUFFIX = "Delegate"

private val NULL = CodeBlock.builder().add("null").build()

class CodeGenerator(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) {
    private val classDeclarations = mutableListOf<KSClassDeclaration>()

    fun add(classDeclaration: KSClassDeclaration) {
        if (classDeclarations.find { it.simpleName.asString() == classDeclaration.simpleName.asString() } == null) classDeclarations.add(classDeclaration)
    }

    fun write() {
        classDeclarations.forEach { writeProxyClass(it) }
    }

    private fun writeProxyClass(classDeclaration: KSClassDeclaration) {
        val className = "${classDeclaration.simpleName.getShortName()}$GENERATED_CLASS_SUFFIX"
        val fileSpec = FileSpec.builder(classDeclaration.packageName.getQualifier(), "$className.kt")
        val classSpec = TypeSpec.classBuilder(className)

        when {
            classDeclaration.classKind == ClassKind.INTERFACE -> {
                classSpec.addSuperinterface(classDeclaration.asType(emptyList()).toTypeName())
            }

            else -> {
                // TODO support for open classes ?
                logger.error("Proxy class must be a interface", classDeclaration)
                return
            }
        }

        val functionDeclarations = classDeclaration.getDeclaredFunctions().toList()
        val properties = classDeclaration.getDeclaredProperties().toList()
        buildConstructor(classSpec, functionDeclarations, properties)

        functionDeclarations.forEach {
            createProxyFunction(it)?.let { proxyFunctionSpec -> classSpec.addFunction(proxyFunctionSpec) }
        }

        val dependencies = Dependencies(aggregating = true, *listOfNotNull(classDeclaration.containingFile).toTypedArray())
        fileSpec.addType(classSpec.build()).build().writeTo(codeGenerator, dependencies)
    }

    private fun createProxyFunction(declaration: KSFunctionDeclaration): FunSpec? {
        if (declaration.isPrivate()) return null

        val name = declaration.simpleName.getShortName()
        val delegateName = "$name$DELEGATE_SUFFIX"
        val returnType = declaration.returnType
        val parameters = getParameterSpecs(declaration)

        val builder = FunSpec.builder(declaration.simpleName.getShortName())
            .addModifiers(KModifier.OVERRIDE)

        if (declaration.hasModifier(KModifier.SUSPEND)) {
            builder.addModifiers(KModifier.SUSPEND)
        }

        parameters.forEach { parameterSpec ->
            builder.addParameter(parameterSpec)
        }

        val codeBlock = CodeBlock.builder()
        if (returnType != null) {
            codeBlock.add("return ")
            builder.returns(returnType.toTypeName())
        }

        // build arguments code block
        val arguments = parameters.map { parameter -> parameter.name }.joinToString(", ")

        // handle default implementation
        val fallbackBody = if (!declaration.isAbstract) "super.$name($arguments)" else NOT_IMPLEMENTED_CODE_BLOCK

        codeBlock.add("$delegateName?.invoke($arguments) ?: $fallbackBody")
        builder.addCode(codeBlock.build())

        return builder.build()
    }
}

private fun buildConstructor(classSpec: TypeSpec.Builder, functionDeclarations: List<KSFunctionDeclaration>, propertyDeclarations: List<KSPropertyDeclaration>) {
    val builder = FunSpec.constructorBuilder()

    propertyDeclarations.forEach { parameterDeclaration ->
        if (parameterDeclaration.isPrivate()) return@forEach

        val name = parameterDeclaration.simpleName.getShortName()
        val type = parameterDeclaration.type.toTypeName()
        builder.addParameter(
            ParameterSpec.builder(name, type).build()
        )

        classSpec.addProperty(
            PropertySpec.builder(name, type, KModifier.OVERRIDE)
                .mutable(parameterDeclaration.isMutable)
                .initializer(name)
                .build()
        )
    }

    functionDeclarations.forEach { functionDeclaration ->
        if (functionDeclaration.isPrivate()) return@forEach

        val name = functionDeclaration.simpleName.getShortName()
        val delegateName = "$name$DELEGATE_SUFFIX"
        val isSuspending = functionDeclaration.hasModifier(KModifier.SUSPEND)

        val parameters = getParameterSpecs(functionDeclaration)
        val lambda = LambdaTypeName.get(
            returnType = functionDeclaration.returnType?.toTypeName() ?: Unit::class.asTypeName(),
            parameters = parameters
        ).copy(nullable = true, suspending = isSuspending)

        builder.addParameter(
            ParameterSpec.builder(delegateName, lambda)
                .defaultValue(NULL)
                .build()
        )
        classSpec.addProperty(
            PropertySpec.builder(delegateName, lambda, KModifier.PRIVATE)
                .initializer(delegateName)
                .build()
        )
    }

    classSpec.primaryConstructor(builder.build())
}

private fun getParameterSpecs(functionDeclaration: KSFunctionDeclaration): List<ParameterSpec> = functionDeclaration.parameters.mapNotNull {
    it.name?.getShortName()?.let { paramName ->
        ParameterSpec.builder(paramName, it.type.toTypeName()).build()
    }
}

fun KSFunctionDeclaration.hasModifier(modifier: KModifier): Boolean {
    return modifiers.any { it.toKModifier() == modifier }
}