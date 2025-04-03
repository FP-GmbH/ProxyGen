package me.fup.joy.proxygen.compiler

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
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

private const val GENERATED_CLASS_SUFFIX = "Proxy"
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
        var functionDeclarations = classDeclaration.getDeclaredFunctions().toList()
        var constructorParameter = mutableListOf<KSValueParameter>()

        when {
            classDeclaration.classKind == ClassKind.INTERFACE -> {
                classSpec.addSuperinterface(classDeclaration.asType(emptyList()).toTypeName())
            }

            classDeclaration.classKind == ClassKind.CLASS &&
                    (classDeclaration.hasModifier(KModifier.OPEN) || classDeclaration.hasModifier(KModifier.ABSTRACT)) -> {
                functionDeclarations = functionDeclarations.filter { it.hasModifier(KModifier.OPEN) || it.hasModifier(KModifier.ABSTRACT) }
                classSpec.superclass(classDeclaration.asType(emptyList()).toTypeName())

                classDeclaration.primaryConstructor?.parameters?.forEach { param ->
                    val name = param.name!!.getShortName()
                    constructorParameter.add(param)
                    classSpec.addSuperclassConstructorParameter(CodeBlock.builder().add(name).build())
                }
            }

            else -> {
                logger.error("Unsupported class type. ProxyGen supports interfaces and open or abstract classes.", classDeclaration)
                return
            }
        }

        val properties = classDeclaration.getDeclaredProperties().toList()
        buildConstructor(classSpec, constructorParameter, functionDeclarations, properties)

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
        val arguments = parameters.joinToString(", ") { parameter -> parameter.name }

        // handle default implementation
        val fallbackBody = if (!declaration.isAbstract) "super.$name($arguments)" else NOT_IMPLEMENTED_CODE_BLOCK

        codeBlock.add("$delegateName?.invoke($arguments) ?: $fallbackBody")
        builder.addCode(codeBlock.build())

        return builder.build()
    }
}

private fun buildConstructor(
    classSpec: TypeSpec.Builder,
    parameter: List<KSValueParameter>,
    functionDeclarations: List<KSFunctionDeclaration>,
    propertyDeclarations: List<KSPropertyDeclaration>
) {
    val builder = FunSpec.constructorBuilder()

    parameter.forEach { param ->
        builder.addParameter(
            ParameterSpec.builder(param.name!!.getShortName(), param.type.toTypeName()).build()
        )
    }

    propertyDeclarations.forEach { parameterDeclaration ->
        if (parameterDeclaration.isPrivate()) return@forEach

        val name = parameterDeclaration.simpleName.getShortName()
        val nameDelegate = "$name$DELEGATE_SUFFIX"
        val type = parameterDeclaration.type.toTypeName()
        val typeDelegate = type.copy(nullable = true)

        builder.addParameter(
            ParameterSpec.builder(nameDelegate, typeDelegate)
                .defaultValue(NULL)
                .build()
        )

        classSpec.addProperty(
            PropertySpec.builder(nameDelegate, typeDelegate, KModifier.PRIVATE)
                .mutable(true)
                .initializer(nameDelegate)
                .build()
        )

        classSpec.addProperty(
            PropertySpec.builder(name, type, KModifier.OVERRIDE)
                .mutable(parameterDeclaration.isMutable)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement("return $nameDelegate ?: $NOT_IMPLEMENTED_CODE_BLOCK")
                        .build()
                ).apply {
                    if (parameterDeclaration.isMutable) {
                        setter(
                            FunSpec.setterBuilder()
                                .addParameter(
                                    ParameterSpec.builder("value", type).build()
                                ).addCode("$nameDelegate = value")
                                .build()
                        )
                    }
                }.build()
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

fun KSDeclaration.hasModifier(modifier: KModifier): Boolean {
    return modifiers.any { it.toKModifier() == modifier }
}