package usecase;

import domain.gateway.ContaGateway;
import domain.model.Cliente;
import domain.model.Conta;
import domain.usecase.ContaUseCase;
import infra.database.H2Config;
import infra.gateway.ContaGatewayDB;
import infra.gateway.ContaGatewayLocal;
import org.h2.tools.Server;
import org.junit.*;

import java.sql.SQLException;

public class ContaUseCaseTest {

    private ContaUseCase contaUseCase;
    private ContaGateway contaGateway;

    // @BeforeClass - antes da classe ser instanciada
    @BeforeClass
    public static void beforeClass() throws SQLException {
        ContaGatewayDB contaGateway = new ContaGatewayDB(H2Config.getDataSource());
        contaGateway.createClienteTable();
        contaGateway.createContaTable();

        Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082")
                .start();
    }

    // @Before - antes de CADA teste
    @Before
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
    @After
    public void after() {
    }

    // @AfterClass
    @AfterClass
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
        Assert.assertEquals(valorEsperadoConta1, conta1.getSaldo());

        Double valorEsperadoConta2 = saldoAtualConta2 + 20.0;
        conta2 = contaGateway.findById("2");
        Assert.assertEquals(valorEsperadoConta2, conta2.getSaldo());
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
        Assert.assertEquals(valorEsperado, conta.getSaldo());
    }

     @Test
    public void deveCriarContaCorretamente(){
        //Given
        Cliente cliente3 = new Cliente("Igor", "222.222.222.22");
        Conta conta3 = new Conta("3", cliente3);

        //When
        contaUseCase.criarConta(conta3);

        //Then
        Assert.assertNotNull(conta3);
    }

    @Test
    public void deveBuscarContaCorretamente(){
        //Given
        String idConta = "1";
        //When
        Conta conta = contaUseCase.buscarConta(idConta);
        //Then
        Assert.assertNotNull(conta);
        Assert.assertEquals(idConta,conta.getId());

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

            Assert.assertEquals(conta.getSaldoDisponivelParaEmprestimo(), (saldoInicialEmprestimo - valor), 0);
            Assert.assertEquals(conta.getSaldo(), (saldoInicial + valor), 0);
        } catch (Exception e) {
            Assert.assertEquals("Conta invalida - [id: " + conta.getId() + "]", e.getMessage());
        }
    }
}
