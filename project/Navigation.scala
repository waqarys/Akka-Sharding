import sbt._
import sbt.Keys._
import complete._
import DefaultParsers._

object Navigation extends AutoPlugin {

  override val trigger = allRequirements

  val solutionDir = new File("../../solutions/scala")
  val testPath = "src/test"
  val mainPath = "src/main"

  val exerciseParser: Parser[String] = Space ~> StringBasic.examples(FixedSetExamples(IO.listFiles(solutionDir).map(_.name)))

  val pullTestsHelp = Help(
    "pullTests",
    ("pullTests <exerciseName>", "Pulls all tests for the specified exercise."),
    "Pulls all tests for the specified exercise."
  )

  val pullTests = Command("pullTests", pullTestsHelp)(state => exerciseParser) {
    (state, arg) =>
      val exerciseDir = new File(solutionDir, arg)
      val testSource = new File(exerciseDir, testPath)
      val testDestination = new File(testPath)

      if(testSource.exists()) {
        println(s"PULLING TESTS FROM $testSource INTO $testDestination")
        IO.delete(testDestination)
        IO.copyDirectory(testSource, testDestination)
      } else {
        println(s"EXERCISE FOLDER NOT FOUND $testSource")
      }

      state
  }

  val pullSolutionHelp = Help(
    "pullSolution",
    ("pullSolution <exerciseName>", "Pulls the solution for the specified exercise. NOTE: This will overwrite your code!!!"),
    "Pulls the solution for the specified exercise. NOTE: This will overwrite your code!!!"
  )

  val pullSolution = Command("pullSolution", pullSolutionHelp)(state => exerciseParser) {
    (state, arg) =>
      val exerciseDir = new File(solutionDir, arg)
      val mainSource = new File(exerciseDir, mainPath)
      val mainDestination = new File(mainPath)

      if(mainSource.exists()) {
        println(s"PULLING SOLUTION FROM $mainSource INTO $mainDestination")
        IO.delete(mainDestination)
        IO.copyDirectory(mainSource, mainDestination)
      } else {
        println(s"SOLUTION FOLDER NOT FOUND $mainSource")
      }

      state
  }

  override lazy val globalSettings =
    Seq(
      commands in Global ++= Seq(
        pullTests,
        pullSolution
      )
    )
}
