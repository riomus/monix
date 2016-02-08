package monix.execution

import minitest.SimpleTestSuite
import monix.execution.schedulers.TestScheduler
import scala.util.control.NonFatal

object RunLoopSuite extends SimpleTestSuite {
  test("RunLoop.start should work") {
    implicit val s = TestScheduler()
    assert(!RunLoop.isAlwaysAsync, "!isAlwaysAsync")

    var triggered = 0

    RunLoop.start { frameId =>
      try {
        triggered += 1
      } catch {
        case NonFatal(ex) =>
          triggered = -1
      }
    }

    assertEquals(triggered, 1)
  }

  test("RunLoop.step should jump threads on barriers") {
    implicit val s = TestScheduler()
    assert(!RunLoop.isAlwaysAsync, "!isAlwaysAsync")

    var triggered = 0
    val barrier = s.batchedExecutionModulus

    RunLoop.step(1) { frameId =>
      try {
        triggered += 1
      } catch {
        case NonFatal(ex) =>
          triggered = -1
      }
    }

    assertEquals(triggered, 1)

    RunLoop.step(barrier-1) { frameId =>
      try {
        triggered += 1
      } catch {
        case NonFatal(ex) =>
          triggered = -1
      }
    }

    assertEquals(triggered, 2)

    var lastFrameId = -1
    RunLoop.step(barrier) { frameId =>
      try {
        lastFrameId = frameId
        triggered += 1
      } catch {
        case NonFatal(ex) =>
          triggered = -1
      }
    }

    assertEquals(triggered, 2)
    assertEquals(lastFrameId, -1)
    s.tick()
    assertEquals(triggered, 3)
    assertEquals(lastFrameId, 0)
  }

  test("RunLoop should always execute async if recommendedBatchSize == 1") {
    implicit val s = TestScheduler(recommendedBatchSize = 1)
    assert(RunLoop.isAlwaysAsync, "isAlwaysAsync")

    var triggered = 0

    RunLoop.start(_ => triggered += 1)
    assertEquals(triggered, 0)

    RunLoop.step(1) { _ => triggered += 1 }
    assertEquals(triggered, 0)

    RunLoop.step(2) { _ => triggered += 1 }
    assertEquals(triggered, 0)

    s.tick()
    assertEquals(triggered, 3)
  }

  test("RunLoop.startAsync should work") {
    implicit val s = TestScheduler(recommendedBatchSize = 1)
    assert(RunLoop.isAlwaysAsync, "isAlwaysAsync")

    var triggered = 0
    var frameAcc = 0

    RunLoop.startAsync { id => triggered += 1; frameAcc += id }
    RunLoop.startAsync { id => triggered += 1; frameAcc += id }
    RunLoop.startAsync { id => triggered += 1; frameAcc += id }

    assertEquals(triggered, 0)
    assertEquals(frameAcc, 0)

    s.tick()
    assertEquals(triggered, 3)
    assertEquals(frameAcc, 0)
  }
}