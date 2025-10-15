package com.example.paradigmaapp.exception

sealed class Failure {
    object NetworkConnection : Failure()
    object ServerError : Failure()
    data class CustomError(val code: Int, val message: String) : Failure()

    /** * Extend this class for feature specific failures.*/
    abstract class FeatureFailure : Failure()
}
