package com.akka_utils.slick.codegen

import com.akka_utils.slick.codegen.TaggedExt.{EnumMetaModel, TaggedMetaModel}
import slick.codegen.SourceCodeGenerator

/**
  * @author iRevThis
  */
trait TaggedExt {
  self: SourceCodeGenerator =>

  def generateSharedTypes(types: Map[String, String]): Seq[TaggedMetaModel] =
    types
      .map { case (typeName, columnType) => TaggedMetaModel("", typeName, columnType) }
      .toSeq

  def enumClasses(enumMapping: Map[String, Map[String, String]]): Set[EnumMetaModel] =
    enumMapping
      .flatMap { case (_, mapping) => mapping.values }
      .toSet
      .map(EnumMetaModel)

}

object TaggedExt {

  trait ColumnMapping {
    def mappingFunction: String
    def columnMapper: String
  }

  lazy val enumMapperFunctionName = "enumMapper"

/*
  FIXME
  lazy val enumMapperFunction =
    s"def $enumMapperFunctionName[X <: ${classOf[EnumLike].getName}](enumClazz: ${classOf[EnumHolder[_]].getName}[X]): Int => X = { code: Int =>\n" +
      "  enumClazz.byCode(code) getOrElse (throw new IllegalArgumentException(s\"Enum[${enumClazz.getClass.getSimpleName}] with code $code is not found\"))\n" +
      "}"*/

  def enumMapper(enumHolderClazz: String): String =
    s"$enumMapperFunctionName($enumHolderClazz)"

  case class EnumMetaModel(enumClazz: String) extends ColumnMapping {
    val typeName = enumClazz.drop(enumClazz.lastIndexOf(".") + 1)

    val mappingFunctionName = typeName + "MapperFunction"

    val mappingFunction = s"""val $mappingFunctionName = { code: Int => $enumClazz.byCode(code) getOrElse (throw new IllegalArgumentException(s"Enum[$typeName] with code $$code is not found")) }"""

    val columnMapper = s"implicit lazy val ${typeName}EnumMapper: BaseColumnType[$enumClazz] = MappedColumnType.base[$enumClazz, Int](item => item.code, ${enumMapper(enumClazz)})"
  }

  case class TaggedMetaModel(prefix: String, columnNameRaw: String, columnType: String) extends ColumnMapping {

    //uuid
    val columnName = if (columnNameRaw == "uuid") "ID" else columnNameRaw

    //GatewayID
    val typeName = prefix + columnName.capitalize

    //GatewayIDTag
    val `trait` = typeName + "Tag"

    //GatewayID = java.util.UUID @@ GatewayIDTag
    val `type` = s"""$typeName = $columnType @@ ${`trait`}"""

    //GatewayID(value: java.util.UUID): GatewayID = Tag[java.util.UUID, GatewayIDTag](value)
    val `def` = s"""$typeName(value: $columnType): $typeName = Tag[$columnType, ${`trait`}](value)"""

    val template =
      s"""
        |trait ${`trait`}
        |type ${`type`}
        |def ${`def`}
      """.stripMargin

    val mappingFunction = typeName

    val columnMapper = s"implicit lazy val ${typeName}Mapper: BaseColumnType[$typeName] = MappedColumnType.base[$typeName, $columnType](Tag.unwrap, $mappingFunction)"
  }

}
