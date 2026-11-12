# Verificação ponta a ponta no navegador

O script [verify-browser.cjs](../scripts/verify-browser.cjs) usa Chromium/Playwright contra uma API real com H2 em memória. Não simula respostas HTTP e não deve ser executado contra o Supabase. É necessário iniciar uma instância nova, sem pedidos anteriores, para repetir o cenário com as mesmas datas.

Terminal 1 (na pasta `aluguel`, com `.env` local válido para o launcher; as propriedades abaixo substituem o datasource por H2):

```bash
./run.sh -Dspring-boot.run.useTestClasspath=true '-Dspring-boot.run.arguments=--server.port=8082 --spring.datasource.url=jdbc:h2:mem:rota_browser;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa --spring.datasource.password= --spring.flyway.locations=classpath:db/migration --app.demo.enabled=true --app.demo.password=BrowserTest123'
```

Essa senha é uma fixture pública do banco descartável em memória, nunca uma senha para configurar no Supabase. O uso do classpath de testes disponibiliza o driver H2. Aguarde inicialização e carga de demonstração.

Terminal 2 (na pasta do frontend):

```bash
API_TARGET=http://localhost:8082 npm run dev -- --port 5175 --strictPort
```

Terminal 3 (na raiz):

```bash
node scripts/verify-browser.cjs
```

O script requer Playwright e Chromium disponíveis no ambiente de verificação. Se instalados em outro local, informe `PLAYWRIGHT_MODULE` com o caminho do módulo e, se necessário, `CHROME_PATH` com o executável. Não são dependências exigidas para rodar o site.

Cenário: login real; edição de cliente; pedido com crédito; aprovação pela empresa; aprovação pelo banco; aceite pelo cliente; dois documentos; bloqueio de exclusão de cliente com histórico; cadastro e exclusão de conta sem vínculos; edição em viewport de 375 px; ausência de erros JavaScript. Capturas ficam na pasta temporária do sistema.

Na execução de 11/11/2026 houve ajustes nos seletores do teste para rótulos que incluem dicas; o cenário final passou. Capturas com dados fictícios da execução estão em [imagens](imagens/).

A validação de layout e jornada em H2 não substitui testes de concorrência e jornada completa contra PostgreSQL nem uma verificação da implantação pública.
