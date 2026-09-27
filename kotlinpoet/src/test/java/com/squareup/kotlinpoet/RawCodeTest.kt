package com.squareup.kotlinpoet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RawCodeTest {
  @Test fun rawCodePlacementAndRoundTrip() {
    val type = TypeSpec.classBuilder("Example")
      .addKdoc("Example documentation.\n")
      .addProperty(PropertySpec.builder("date", java.util.Date::class).initializer("%T()", java.util.Date::class).build())
      .addType(TypeSpec.classBuilder("Nested").build())
      .addRawMembers("fun x(): String {\n    return \"%L %S %T\"\n}")
      .addRawMembers("fun value(x: String): String = \"\$x\"\n")
      .build()
    val file = FileSpec.builder("example", "Example")
      .indent("    ")
      .addRawImports("import java.util.*")
      .addRawImports("import java.util.concurrent.*\n")
      .addType(type)
      .build()
    val source = file.toString()
    assertTrue(source.contains("import java.util.Date\nimport java.util.*\nimport java.util.concurrent.*\n"))
    assertTrue(source.indexOf("import java.util.*") < source.indexOf("/**"))
    assertTrue(source.indexOf("class Nested") < source.indexOf("fun x()"))
    assertTrue(source.contains("    fun x(): String {\n        return \"%L %S %T\"\n    }\n"))
    assertTrue(source.endsWith("    fun value(x: String): String = \"\$x\"\n\n}\n"))
    assertEquals(source, file.toBuilder().build().toString())
    assertEquals(type.toString(), type.toBuilder().build().toString())
  }

  @Test fun rawImportsWithoutGeneratedImports() {
    val file = FileSpec.builder("", "Example")
      .addRawImports("import java.util.*")
      .addType(TypeSpec.classBuilder("Example").addRawMembers("val date = Date()").build())
      .build()
    assertTrue(file.toString().startsWith("import java.util.*\n\n"))
    assertTrue(file.toString().contains("val date = Date()\n"))
  }

  @Test fun emptyAdditionsPreserveOutput() {
    val type = TypeSpec.classBuilder("Example").build()
    assertEquals(type.toString(), type.toBuilder().addRawMembers("").build().toString())
    val file = FileSpec.builder("example", "Example").addType(type).build()
    assertEquals(file.toString(), file.toBuilder().addRawImports("").build().toString())
  }

  @Test fun enumRawMembersAndConstantBodies() {
    val type = TypeSpec.enumBuilder("Example")
      .addEnumConstant("VALUE", TypeSpec.anonymousClassBuilder()
        .addRawMembers("override fun x(): String = \"%L\"").build())
      .addRawMembers("abstract fun x(): String")
      .build()
    val source = type.toString()
    assertTrue(source.contains("VALUE {\n"))
    assertTrue(source.contains("override fun x(): String = \"%L\"\n"))
    assertTrue(source.contains("};\n"))
    assertTrue(source.endsWith("abstract fun x(): String\n\n}\n"))
  }

  @Test fun otherwiseEmptyTypesRetainRawMembers() {
    val types = listOf(
      TypeSpec.classBuilder("Example"),
      TypeSpec.objectBuilder("Example"),
      TypeSpec.interfaceBuilder("Example"),
      TypeSpec.anonymousClassBuilder()
    )
    for (builder in types) {
      val source = builder.addRawMembers("fun x(): String = \"%T\"").build().toString()
      assertTrue(source.contains("{\n"))
      assertTrue(source.contains("fun x(): String = \"%T\"\n"))
      assertTrue(source.trimEnd().endsWith("}"))
    }
  }
}
