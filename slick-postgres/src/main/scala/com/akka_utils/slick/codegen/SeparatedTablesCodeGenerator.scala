package com.akka_utils.slick.codegen

import com.typesafe.config.{Config, ConfigFactory}
import com.akka_utils.slick.codegen.TaggedExt.{EnumMetaModel, TaggedMetaModel}
import com.akka_utils.slick.postgres.PostgresProfileExtended
import slick.codegen.SourceCodeGenerator
import slick.jdbc.JdbcProfile
import slick.model.Model
import slick.sql.SqlProfile.ColumnOption

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalaz._
import Scalaz._

/**
  * @author Maksim Ochenashko
  */
class SeparatedTablesCodeGenerator(metamodel: Model, outputDir: String, pkg: String,
                                   taggedTypesMapping: Map[String, Seq[String]],
                                   sharedTypes: Map[String, String],
                                   sharedTypesMapping: Map[String, Map[String, String]],
                                   enumMapping: Map[String, Map[String, String]]) extends SourceCodeGenerator(metamodel) with TaggedExt {

  for (table <- tables) {
    writeStringToFile(tableFileContent(table.asInstanceOf[TableExt]), outputDir, pkg, s"${table.TableClass.name}Table.scala")
  }

  override def code: String = {
    if (ddlEnabled) {
      "\n/** DDL for all tables. Call .create to execute. */" +
        (
          if (tables.length > 5)
            "\nlazy val schema: profile.SchemaDescription = Array(" + tables.map(_.TableValue.name + ".schema").mkString(", ") + ").reduceLeft(_ ++ _)"
          else if (tables.nonEmpty)
            "\nlazy val schema: profile.SchemaDescription = " + tables.map(_.TableValue.name + ".schema").mkString(" ++ ")
          else
            "\nlazy val schema: profile.SchemaDescription = profile.DDL(Nil, Nil)"
          ) +
        "\n@deprecated(\"Use .schema instead of .ddl\", \"3.0\")" +
        "\ndef ddl = schema" +
        "\n\n"
    } else ""
  }

  private def tableFileContent(table: TableExt): String = {
    val clazzName = table.TableClass.name + "Table"
    val taggedColumns = table.taggedColumns
    val taggedTypesEnabled = table.taggedTypesEnabled

    s"""package $pkg
        |
        |${table.hlistEnabled ? HListImports | ""}
        |${table.PlainSqlMapper.enabled ? PlainSqlMapperImport | ""}
        |${taggedTypesEnabled ? "import scalaz._" | ""}
        |
        |trait $clazzName {
        |  self: Tables =>
        |
        |  import profile.api._
        |  ${taggedTypesEnabled ? s"import $clazzName._" | ""}
        |
        |  ${taggedTypesEnabled ? indent(taggedColumns.map(_.columnMapper).mkString("\n")) | ""}
        |
        |  ${indent(table.code.mkString("\n"))}
        |
        |}
        |
        |${taggedTypesEnabled ? taggedTypesCompanion(clazzName, taggedColumns) | ""}
    """.stripMargin
  }

  protected def taggedTypesCompanion(clazzName: String, taggedColumns: Seq[TaggedMetaModel]): String =
    s"object $clazzName {\n" + indent(taggedColumns.map(_.template).mkString("\n")) + "\n}"

  trait TaggedModelColumn {
    def taggedModel: Option[TaggedMetaModel]
  }

  class TableExt(tbl: slick.model.Table) extends super.TableDef(tbl) {
    table =>

    def taggedTypesEnabled = taggedTypesMapping.get(tbl.name.table).exists(_.nonEmpty)

    def taggedColumns: Seq[TaggedMetaModel] =
      columns.collect { case c: TaggedModelColumn if c.taggedModel.isDefined => c.taggedModel.get }

    override def Column = new ColumnExtended(_)

    override def compoundType(types: Seq[String]): String = super.compoundType(types)

    class ColumnExtended(model: slick.model.Column) extends super.ColumnDef(model) with TaggedModelColumn {
      column =>
      // customize db type -> scala type mapping, pls adjust it according to your environment
      override def rawType: String =
        (fkType orElse taggedModel.map(_.typeName) orElse sharedTypeModel orElse enumModel.map(_.enumClazz)) | customType(model)

      private def customType(mdl: slick.model.Column): String =
        mdl.tpe match {
          case "java.sql.Date" => "java.time.LocalDate"
          case "java.sql.Time" => "java.time.LocalTime"
          case "java.sql.Timestamp" => "java.time.LocalDateTime"
          case "String" =>
            mdl.options
              .collectFirst { case x: ColumnOption.SqlType if x.typeName == "jsonb" => "io.circe.Json" }
              .getOrElse("String")
          case _ => parseType(mdl.tpe)
        }

      val fkType: Option[String] = table.foreignKeys.find(_.model.referencingColumns.exists(_.name == model.name)) flatMap { x =>
        val selfColumns = x.model.referencingColumns

        val refTable = x.model.referencedTable.table
        val refColumns = x.model.referencedColumns

        val includedRefColumns = taggedTypesMapping.get(refTable) | Nil

        (selfColumns zip refColumns).collectFirst { case (self, ref) if self.name == model.name && includedRefColumns.contains(ref.name) =>
          val refTableName = tableName(refTable)
          //todo check here for shared instance
          refTableName + "Table." + TaggedMetaModel(refTableName.stripSuffix("s"), ref.name, customType(ref)).typeName
        }
      }

      val taggedModel = taggedTypesMapping.get(table.model.name.table).exists(_.contains(model.name)) option {
        TaggedMetaModel(table.TableClass.name.stripSuffix("s"), column.name, customType(model))
      }

      val sharedTypeModel = sharedTypesMapping.get(table.model.name.table)
        .flatMap(_.find(_._1 == model.name))
        .map { case (columnName, sharedType) => s"Tables.$sharedType" }

      val enumModel = enumMapping.get(table.model.name.table)
        .flatMap(_.find(_._1 == model.name))
        .map { case (columnName, enum) => EnumMetaModel(enum) }

      override def defaultCode: (Any) => String =
        enumModel match {
          case None =>
            super.defaultCode

          case Some(enum) =>
            {
              case v: Int =>
                s"${TaggedExt.enumMapper(enum.enumClazz)} apply $v"

              case any =>
                super.defaultCode(any)
            }
        }


    }

  }

  override def Table = new TableExt(_)

  // ensure to use our customized postgres driver at `import profile.simple._`
  override def packageCode(profile: String, pkg: String, container: String, parentType: Option[String]): String = {
    val firstMixType = parentType.map(_ => "with ") getOrElse "extends "
    val mixinCode = tables.map(_.TableClass.name + "Table").mkString(firstMixType, " with ", "")
    s"""package $pkg
        |
       |import scalaz._
        |
       |// AUTO-GENERATED Slick data model
        |/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
        |trait $container${parentType.map(t => s" extends $t").getOrElse("")} $mixinCode {
        |  val profile: $profile
        |  import $container._
        |  import profile.api._
        |  ${indent(code)}
        |
        |  ${/*FIXME indent(TaggedExt.enumMapperFunction)*/}
        |
        |  ${indent(generateSharedTypes(sharedTypes).map(_.columnMapper).mkString("\n"))}
        |  ${indent(enumClasses(enumMapping).map(_.columnMapper).mkString("\n"))}
        |}
        |
       |/** Stand-alone Slick data model for immediate use */
        |object $container extends {
        |  val profile = $profile
        |} with $container {
        |  ${indent(generateSharedTypes(sharedTypes).map(_.template).mkString("\n"))}
        |}
      """.stripMargin
  }

  private[this] lazy val PlainSqlMapperImport =
    "// NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.\n" +
      "import slick.jdbc.{GetResult => GR}\n"

  private[this] lazy val HListImports =
    "import slick.collection.heterogeneous._\n" + "import slick.collection.heterogeneous.syntax._\n"
}

object SeparatedTablesCodeGenerator {

  import scala.collection.JavaConversions._
  import java.io.File

  def main(args: Array[String]): Unit =
    args.toList match {
      case configPath :: outputDir :: Nil =>
        val slickCfg = ConfigFactory.parseFile(new File(configPath)).getConfig("slick")
        val codeGenCfg = slickCfg.getConfig("codegen")

        def getOpt[X](path: String, f: Config => String => X): Option[X] = codeGenCfg.hasPath(path) option f(codeGenCfg)(path)

        val includedTables = getOpt("included-tables", c => p => c.getStringList(p).toSeq)

        val ignoredTables = getOpt("ignored-tables", c => p => c.getStringList(p).toSeq)

        val taggedTypesMapping = getOpt("tagged-types-mapping", c => p => c.getObject(p).unwrapped().mapValues(_.asInstanceOf[java.util.ArrayList[String]].toSeq).toMap)

        val sharedTypes = getOpt("shared-types", c => p => c.getObject(p).unwrapped().mapValues(_.asInstanceOf[String]).toMap)

        val sharedTypesMapping = getOpt("shared-types-mapping", c => p => c.getObject(p).unwrapped().mapValues(_.asInstanceOf[java.util.Map[String, String]].toMap).toMap)

        val enumMapping = getOpt("enum-mapping", c => p => c.getObject(p).unwrapped().mapValues(_.asInstanceOf[java.util.Map[String, String]].toMap).toMap)

        val url = slickCfg.getString("dbs.default.db.url")
        val jdbcDriver = slickCfg.getString("dbs.default.db.driver")
        val slickDriver = slickCfg.getString("dbs.default.driver") stripSuffix "$"
        val pkg = codeGenCfg.getString("package")
        val user = slickCfg.getString("dbs.default.db.user")
        val password = slickCfg.getString("dbs.default.db.password")

        run(slickDriver, jdbcDriver, url, outputDir, pkg, user.some, password.some, includedTables, ignoredTables,
          taggedTypesMapping | Map.empty, sharedTypes | Map.empty, sharedTypesMapping | Map.empty, enumMapping | Map.empty)
      case _ =>
        println("Two arguments are required: configPath and output dir")
        sys exit 1
    }

  def run(slickDriver: String,
          jdbcDriver: String,
          url: String,
          outputDir: String,
          pkg: String,
          user: Option[String],
          password: Option[String],
          includedTables: Option[Seq[String]],
          ignoredTables: Option[Seq[String]],
          taggedTypesMapping: Map[String, Seq[String]],
          sharedTypes: Map[String, String],
          sharedTypesMapping: Map[String, Map[String, String]],
          enumMapping: Map[String, Map[String, String]]): Unit = {
    val driver: JdbcProfile = PostgresProfileExtended
    val dbFactory = driver.api.Database
    val db = dbFactory.forURL(url, driver = jdbcDriver, user = user.orNull, password = password.orNull, keepAliveConnection = true)
    try {
      import scala.concurrent.ExecutionContext.Implicits.global
      val metamodel = driver.defaultTables
        .map { tables =>
          tables filter { t =>
            (includedTables.isEmpty || includedTables.exists(_ contains t.name.name)) &&
              (ignoredTables.isEmpty || !ignoredTables.exists(_ contains t.name.name))
          }
        }
        .flatMap(driver.createModelBuilder(_, ignoreInvalidDefaults = false).buildModel)

      val m = Await.result(db.run(metamodel), Duration.Inf)
      new SeparatedTablesCodeGenerator(m, outputDir, pkg, taggedTypesMapping, sharedTypes, sharedTypesMapping, enumMapping).writeToFile(slickDriver, outputDir, pkg)
    } finally db.close
  }

}
