package com.hms.entity;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.regions.Region;

import java.time.Duration;

/**
 * User entity class.
 *
 * Passwords/credentials are no longer stored as plain-text fields.
 * Sensitive credentials are retrieved at runtime from AWS Secrets Manager
 * using the secret name stored in the password field (e.g. "hms/user/{email}/password").
 * This eliminates hardcoded secrets and prevents credential leakage in logs.
 *
 * cr-java-0097 – Missing Connection Timeouts
 *   The SecretsManagerClient is now built with an explicit
 *   {@link ClientOverrideConfiguration} that sets:
 *     apiCallTimeout        – maximum total time for a single API call (10 s)
 *     apiCallAttemptTimeout – maximum time for each individual attempt  (5 s)
 *   This prevents the client from hanging indefinitely when the Secrets Manager
 *   endpoint is slow or unreachable in cloud environments.
 */
public class User {
	private int id;
	private String fullName;
	private String email;
	/**
	 * Stores the AWS Secrets Manager secret name/ARN for this user's credential,
	 * NOT the plain-text password. Use {@link #resolvePassword()} to retrieve
	 * the actual credential from AWS Secrets Manager at runtime.
	 */
	private String password;

	/** AWS region used for Secrets Manager lookups (configurable via environment variable). */
	private static final String AWS_REGION = System.getenv("AWS_REGION") != null
			? System.getenv("AWS_REGION")
			: "us-east-1";

	// -------------------------------------------------------------------------
	// AWS SDK timeout constants – cr-java-0097
	// -------------------------------------------------------------------------

	/**
	 * Maximum total time allowed for a single AWS Secrets Manager API call,
	 * including all retry attempts (10 seconds).
	 */
	private static final Duration SECRETS_MANAGER_API_CALL_TIMEOUT         = Duration.ofSeconds(10);

	/**
	 * Maximum time allowed for each individual attempt of an AWS Secrets Manager
	 * API call before it is considered failed and retried (5 seconds).
	 */
	private static final Duration SECRETS_MANAGER_API_CALL_ATTEMPT_TIMEOUT = Duration.ofSeconds(5);

	public User() {
		super();
	}

	public User(int id, String fullName, String email, String password) {
		super();
		this.id = id;
		this.fullName = fullName;
		this.email = email;
		this.password = password;
	}

	public User(String fullName, String email, String password) {
		super();
		this.fullName = fullName;
		this.email = email;
		this.password = password;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Returns the secret name/ARN stored for this user's credential.
	 * This is NOT the plain-text password — it is the reference key used
	 * to look up the actual credential in AWS Secrets Manager.
	 *
	 * @return the secret name/ARN
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Stores the secret name/ARN for this user's credential in AWS Secrets Manager.
	 * Do NOT pass a plain-text password here; pass the Secrets Manager secret name/ARN.
	 *
	 * @param password the AWS Secrets Manager secret name or ARN
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Resolves the actual credential value by fetching it from AWS Secrets Manager
	 * at runtime using the secret name/ARN stored in {@link #password}.
	 *
	 * The {@link SecretsManagerClient} is configured with explicit API call and
	 * per-attempt timeouts (cr-java-0097) to prevent indefinite hangs when the
	 * Secrets Manager endpoint is slow or unreachable in cloud environments.
	 *
	 * @return the plain-text secret value retrieved from AWS Secrets Manager
	 * @throws RuntimeException if the secret cannot be retrieved
	 */
	public String resolvePassword() {
		if (this.password == null || this.password.isEmpty()) {
			throw new IllegalStateException("No secret name/ARN configured for user: " + this.email);
		}

		// cr-java-0097: Build ClientOverrideConfiguration with explicit timeouts
		// to prevent the AWS SDK from hanging indefinitely on network issues.
		ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
				// Total time budget for the entire API call (all retries included).
				.apiCallTimeout(SECRETS_MANAGER_API_CALL_TIMEOUT)
				// Time budget for each individual attempt before it is retried.
				.apiCallAttemptTimeout(SECRETS_MANAGER_API_CALL_ATTEMPT_TIMEOUT)
				.build();

		try (SecretsManagerClient client = SecretsManagerClient.builder()
				.region(Region.of(AWS_REGION))
				.overrideConfiguration(overrideConfig)
				.build()) {
			GetSecretValueRequest request = GetSecretValueRequest.builder()
					.secretId(this.password)
					.build();
			GetSecretValueResponse response = client.getSecretValue(request);
			return response.secretString();
		} catch (Exception e) {
			throw new RuntimeException(
					"Failed to retrieve secret from AWS Secrets Manager for user: " + this.email, e);
		}
	}

	/**
	 * toString() intentionally omits the password/secret-name field to prevent
	 * accidental credential leakage in application logs or stack traces.
	 */
	@Override
	public String toString() {
		return "User [id=" + id + ", fullName=" + fullName + ", email=" + email + "]";
	}
}
