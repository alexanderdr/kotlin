/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:kotlin.jvm.JvmName("CoroutinesKt")
package kotlin.coroutines.experimental

import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn

/**
 * Starts coroutine with receiver type [R] and result type [T].
 * This function creates and start a new, fresh instance of suspendable computation every time it is invoked.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <R, T> (suspend R.() -> T).startCoroutine(
        receiver: R,
        completion: Continuation<T>
) {
    createCoroutineInternal(receiver, completion).resume(Unit)
}

/**
 * Starts coroutine without receiver and with result type [T].
 * This function creates and start a new, fresh instance of suspendable computation every time it is invoked.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <T> (suspend  () -> T).startCoroutine(
        completion: Continuation<T>
) {
    createCoroutineInternal(completion).resume(Unit)
}

/**
 * Creates coroutine with receiver type [R] and result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <R, T> (suspend R.() -> T).createCoroutine(
        receiver: R,
        completion: Continuation<T>
): Continuation<Unit> = SafeContinuation(createCoroutineInternal(receiver, completion), COROUTINE_SUSPENDED)

/**
 * Creates coroutine without receiver and with result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <T> (suspend () -> T).createCoroutine(
        completion: Continuation<T>
): Continuation<Unit> = SafeContinuation(createCoroutineInternal(completion), COROUTINE_SUSPENDED)

/**
 * Obtains the current continuation instance inside suspend functions and suspends
 * currently running coroutine.
 *
 * In this function both [Continuation.resume] and [Continuation.resumeWithException] can be used either synchronously in
 * the same stack-frame where suspension function is run or asynchronously later in the same thread or
 * from a different thread of execution. Repeated invocation of any resume function produces [IllegalStateException].
 */
@SinceKotlin("1.1")
public inline suspend fun <T> suspendCoroutine(crossinline block: (Continuation<T>) -> Unit): T =
        suspendCoroutineOrReturn { c: Continuation<T> ->
            val safe = SafeContinuation(c)
            block(safe)
            safe.getResult()
        }

// INTERNAL DECLARATIONS

internal inline fun processInvokeCallOnCoroutine(completion: Continuation<*>, block: () -> Any?) {
    try {
        val result = block()
        if (result !== COROUTINE_SUSPENDED) {
            @Suppress("UNCHECKED_CAST")
            (completion as Continuation<Any?>).resume(result)
        }
    } catch (t: Throwable) {
        completion.resumeWithException(t)
    }
}