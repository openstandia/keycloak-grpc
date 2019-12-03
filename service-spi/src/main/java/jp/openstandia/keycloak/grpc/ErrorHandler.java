package jp.openstandia.keycloak.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

public class ErrorHandler {

    public static boolean hasError(Response response) {
        return response != null && response.getStatus() != 200;
    }

    public static StatusRuntimeException convert(Response response) {
        if (response.getStatusInfo() == Response.Status.CONFLICT) {
            return Status.ALREADY_EXISTS
                    .withDescription(response.getStatusInfo().getReasonPhrase())
                    .asRuntimeException();
        }
        if (response.getStatusInfo() == Response.Status.BAD_REQUEST) {
            return Status.INVALID_ARGUMENT
                    .withDescription(response.getStatusInfo().getReasonPhrase())
                    .asRuntimeException();
        }
        if (response.getStatusInfo() == Response.Status.NOT_FOUND) {
            return Status.NOT_FOUND
                    .withDescription(response.getStatusInfo().getReasonPhrase())
                    .asRuntimeException();
        }
        if (response.getStatusInfo() == Response.Status.FORBIDDEN) {
            return Status.PERMISSION_DENIED
                    .withDescription(response.getStatusInfo().getReasonPhrase())
                    .asRuntimeException();
        }
        if (response.getStatusInfo() == Response.Status.INTERNAL_SERVER_ERROR) {
            return Status.INTERNAL
                    .withDescription(response.getStatusInfo().getReasonPhrase())
                    .asRuntimeException();
        }
        if (response.getStatusInfo() == Response.Status.PRECONDITION_FAILED) {
            return Status.FAILED_PRECONDITION
                    .withDescription(response.getStatusInfo().getReasonPhrase())
                    .asRuntimeException();
        }

        return Status.UNKNOWN
                .withDescription(response.getStatusInfo().getReasonPhrase())
                .asRuntimeException();
    }

    public static StatusRuntimeException convert(RuntimeException e) {
        if (e instanceof NotAuthorizedException) {
            return Status.UNAUTHENTICATED
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException();
        }
        if (e instanceof NotFoundException) {
            return Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException();
        }
        if (e instanceof BadRequestException) {
            return Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException();
        }
        if (e instanceof ForbiddenException) {
            return Status.PERMISSION_DENIED
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException();
        }
        if (e instanceof InternalServerErrorException) {
            return Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException();
        }

        // UNKNOWN
        throw e;
    }
}
