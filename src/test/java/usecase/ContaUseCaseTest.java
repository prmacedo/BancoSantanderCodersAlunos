package usecase;

import domain.gateway.ContaGateway;
import domain.model.Cliente;
import domain.model.Conta;
import domain.usecase.ContaUseCase;
import infra.database.H2Config;
import infra.gateway.ContaGatewayDB;
import org.h2.tools.Server;
import org.junit.jupiter.api.*;

//Desafio - Grupo 15/05 - Santander 1111
//Atividade - Grupo 15/05 - Santander 1111

import java.sql.SQLException;

public class ContaUseCaseTest {

    private ContaUseCase contaUseCase;
    private ContaGateway contaGateway;

    // @BeforeClass - antes da classe ser instanciada
    @BeforeAll
    public static void beforeClass() throws SQLException {
        ContaGatewayDB contaGateway = new ContaGatewayDB(H2Config.getDataSource());
        contaGateway.createClienteTable();
        contaGateway.createContaTable();

        Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082")
                .start();
    }

    // @Before - antes de CADA teste
    @BeforeEach
    public void before() {
        contaGateway = new ContaGatewayDB(H2Config.getDataSource());
        contaUseCase = new ContaUseCase(contaGateway);

        Cliente cliente1 = new Cliente("1", "Ana", "111.111.111.11");
        Conta conta1 = new Conta("1", cliente1);
        conta1.adicionarSaldoParaEmprestimo(1_000d);

        Cliente cliente2 = new Cliente("2", "Carla", "222.222.222.22");
        Conta conta2 = new Conta("2", cliente2);

        contaGateway.save(conta1);
        contaGateway.save(conta2);
    }

    // @After
    @AfterEach
    public void after() {
    }

    // @AfterClass
    @AfterAll
    public static void afterClass() {
    }

    @Test
    public void deveTransferirCorretamenteEntreDuasContas() throws Exception {
        // Given - Dado
        contaUseCase.depositar("1", 100.0);
        Conta conta1 = contaGateway.findById("1");
        Conta conta2 = contaGateway.findById("2");
        Double saldoAtualConta1 = conta1.getSaldo();
        Double saldoAtualConta2 = conta2.getSaldo();

        // When - Quando
        contaUseCase.transferir("1", "2", 20.0);

        // Then - Entao
        Double valorEsperadoConta1 = saldoAtualConta1 - 20.0;
        conta1 = contaGateway.findById("1");
        Assertions.assertEquals(valorEsperadoConta1, conta1.getSaldo());

        Double valorEsperadoConta2 = saldoAtualConta2 + 20.0;
        conta2 = contaGateway.findById("2");
        Assertions.assertEquals(valorEsperadoConta2, conta2.getSaldo());
    }

    @Test
    public void deveDepositarCorretamente() throws Exception {
        // Given -  Dado
        Conta conta = contaGateway.findById("1");
        Double saldoAtual = conta.getSaldo();

        // When - Quando
        contaUseCase.depositar("1", 10.0);
        conta = contaGateway.findById("1");

        // Then
        Double valorEsperado = 10.0 + saldoAtual;
        Assertions.assertEquals(valorEsperado, conta.getSaldo());
    }

     @Test
    public void deveCriarContaCorretamente(){
        //Given
        Cliente cliente3 = new Cliente("3", "Igor", "222.222.222.22");
        Conta conta3 = new Conta("3", cliente3);

        //When
        contaUseCase.criarConta(conta3);

        //Then
         Assertions.assertNotNull(conta3);
    }

    @Test
    public void deveBuscarContaCorretamente(){
        //Given
        String idConta = "1";
        //When
        Conta conta = contaUseCase.buscarConta(idConta);
        //Then
        Assertions.assertNotNull(conta);
        Assertions.assertEquals(idConta,conta.getId());

    }

    @Test
    public void deveEmprestarCorretamente() throws Exception {
        Conta conta = contaUseCase.buscarConta("1");
        Double saldoInicial = conta.getSaldo();
        Double saldoInicialEmprestimo = conta.getSaldoDisponivelParaEmprestimo();
        Double valor = 1_000d;

        try {
            contaUseCase.emprestimo(conta.getId(), valor);
            conta = contaUseCase.buscarConta("1");
            Conta contaAtualizada = conta;
            Assertions.assertAll(
                () -> Assertions.assertEquals(contaAtualizada.getSaldoDisponivelParaEmprestimo(), (saldoInicialEmprestimo - valor), 0),
                () -> Assertions.assertEquals(contaAtualizada.getSaldo(), (saldoInicial + valor), 0)
            );
        } catch (Exception e) {
            Assertions.assertEquals("Conta invalida - [id: " + conta.getId() + "]", e.getMessage());
        }
    }
}
