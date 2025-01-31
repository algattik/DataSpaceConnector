package org.eclipse.dataspaceconnector.transfer.dataplane.sync.api.controller;

import org.eclipse.dataspaceconnector.common.token.TokenValidationService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.security.DataEncrypter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceClaimsSchema.DATA_ADDRESS_CLAIM;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataPlaneTransferSyncApiControllerTest {

    private DataEncrypter encrypterMock;
    private TokenValidationService tokenValidationServiceMock;
    private DataPlaneTransferSyncApiController controller;

    @BeforeEach
    void setUp() {
        encrypterMock = mock(DataEncrypter.class);
        tokenValidationServiceMock = mock(TokenValidationService.class);
        var monitor = mock(Monitor.class);
        controller = new DataPlaneTransferSyncApiController(monitor, tokenValidationServiceMock, encrypterMock);
    }

    @Test
    void verifyValidateSuccess() {
        var token = "token-test";
        var claims = createClaims();
        when(tokenValidationServiceMock.validate(token)).thenReturn(Result.success(claims));
        when(encrypterMock.decrypt("encrypted-data-address")).thenReturn("decrypted-data-address");

        var response = controller.validate(token);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(ClaimToken.class);
        var claimsResult = (ClaimToken) response.getEntity();
        assertThat(claimsResult.getClaims())
                .containsEntry("foo", "bar")
                .containsEntry("hello", "world")
                .containsEntry(DATA_ADDRESS_CLAIM, "decrypted-data-address");
    }

    @Test
    void verifyValidateFailure() {
        var token = "token-test";
        when(tokenValidationServiceMock.validate(token)).thenReturn(Result.failure("error"));

        var response = controller.validate(token);

        assertThat(response.getStatus()).isEqualTo(400);
    }

    private static ClaimToken createClaims() {
        return ClaimToken.Builder.newInstance()
                .claims(Map.of("foo", "bar", "hello", "world", DATA_ADDRESS_CLAIM, "encrypted-data-address"))
                .build();
    }
}