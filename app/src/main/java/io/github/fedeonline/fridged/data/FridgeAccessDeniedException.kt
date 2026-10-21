package io.github.fedeonline.fridged.data

/** The backend refused access: the signed-in account lacks the `owner` claim. */
class FridgeAccessDeniedException(cause: Throwable? = null) : Exception(cause)
