package infra.gateway;

import domain.gateway.ContaGateway;
import domain.model.Cliente;
import domain.model.Conta;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ContaGatewayDB implements ContaGateway {
    private DataSource dataSource;
    private static final String FIND_BY_ID =
            "SELECT c.id AS cliente_id, c.nome, c.cpf, co.id AS conta_id, co.saldo, co.saldo_disponivel " +
            "FROM conta co " +
            "JOIN cliente c ON co.cliente_id = c.id " +
            "WHERE co.id = ?";
    private static final String INSERT_CLIENTE =
            "INSERT INTO cliente (id, nome, cpf) VALUES (?, ?, ?)";
    private static final String INSERT_CONTA =
            "INSERT INTO conta (id, cliente_id, saldo, saldo_disponivel) VALUES (?, ?, ?, ?)";
    private static final String DELETE_CONTA = "DELETE FROM conta WHERE id = ?";
    private static final String DELETE_CLIENTE = "DELETE FROM cliente WHERE id = ?";

    public ContaGatewayDB(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Conta findById(String id) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_ID)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Cliente cliente = new Cliente(rs.getString("id"), rs.getString("nome"), rs.getString("cpf"));
                return new Conta(rs.getString("conta_id"), cliente, rs.getDouble("saldo"),
                        rs.getDouble("saldo_disponivel"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Conta save(Conta conta) {
        try (Connection con = dataSource.getConnection()) {
            boolean clienteExistente = clienteExiste(con, conta.getCliente().getId());

            if (clienteExistente) {
                atualizarCliente(con, conta.getCliente());
            } else {
                inserirCliente(con, conta.getCliente());
            }

            if (contaExiste(con, conta.getId())) {
                atualizarConta(con, conta);
            } else {
                inserirConta(con, conta);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conta;
    }

    private boolean clienteExiste(Connection con, String clienteId) throws SQLException {
        String query = "SELECT COUNT(*) FROM cliente WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private void atualizarCliente(Connection con, Cliente cliente) throws SQLException {
        String query = "UPDATE cliente SET nome = ?, cpf = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, cliente.getNome());
            ps.setString(2, cliente.getCpf());
            ps.setString(3, cliente.getId());
            ps.executeUpdate();
        }
    }

    private void inserirCliente(Connection con, Cliente cliente) throws SQLException {
        String query = "INSERT INTO cliente (id, nome, cpf) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, cliente.getId());
            ps.setString(2, cliente.getNome());
            ps.setString(3, cliente.getCpf());
            ps.executeUpdate();
        }
    }

    private boolean contaExiste(Connection con, String contaId) throws SQLException {
        String query = "SELECT COUNT(*) FROM conta WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, contaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private void atualizarConta(Connection con, Conta conta) throws SQLException {
        String query = "UPDATE conta SET saldo = ?, saldo_disponivel = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setDouble(1, conta.getSaldo());
            ps.setDouble(2, conta.getSaldoDisponivelParaEmprestimo());
            ps.setString(3, conta.getId());
            ps.executeUpdate();
        }
    }

    private void inserirConta(Connection con, Conta conta) throws SQLException {
        String query = "INSERT INTO conta (id, cliente_id, saldo, saldo_disponivel) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, conta.getId());
            ps.setString(2, conta.getCliente().getId());
            ps.setDouble(3, conta.getSaldo());
            ps.setDouble(4, conta.getSaldoDisponivelParaEmprestimo());
            ps.executeUpdate();
        }
    }

    public void delete(String idConta, String idCliente) {
        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(DELETE_CONTA)) {
                ps.setString(1, idConta);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(DELETE_CLIENTE)) {
                ps.setString(1, idCliente);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createContaTable() {
        String query = "CREATE TABLE IF NOT EXISTS conta (" +
                "id VARCHAR(50) PRIMARY KEY, " +
                "cliente_id VARCHAR(50), " +
                "saldo DOUBLE, " +
                "saldo_disponivel DOUBLE, " +
                "FOREIGN KEY (cliente_id) REFERENCES cliente(id))";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createClienteTable() {
        String query = "CREATE TABLE IF NOT EXISTS cliente (" +
                "id VARCHAR(50) PRIMARY KEY, " +
                "nome VARCHAR(255), " +
                "cpf VARCHAR(14))";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
