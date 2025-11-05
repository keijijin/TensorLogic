package ai.tensorlogic;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.servers.Server;

/**
 * JAX-RS Application クラス
 * 
 * OpenAPI定義とSwagger UIの設定
 */
@ApplicationPath("/")
@OpenAPIDefinition(
    info = @Info(
        title = "LLM Tensor Logic Integration API",
        version = "1.0.0",
        description = "LLMの推論をTensor Logicで検証するエンタープライズAPIです。",
        contact = @Contact(
            name = "Tensor Logic Team",
            email = "support@tensorlogic.ai"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "開発環境")
    }
)
public class TensorLogicApplication extends Application {
    // Quarkusが自動的に設定を行うため、メソッドの実装は不要
}

