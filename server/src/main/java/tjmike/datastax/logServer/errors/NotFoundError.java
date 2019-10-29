package tjmike.datastax.logServer.errors;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND, reason="Unknown request")
class NotFoundError extends RuntimeException {
	public NotFoundError(String message) {
		super(message);
	}
}


