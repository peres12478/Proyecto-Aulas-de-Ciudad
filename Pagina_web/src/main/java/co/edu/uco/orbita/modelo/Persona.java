package co.edu.uco.orbita.modelo;

import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Identidad académica abstracta; la contraseña nunca se conserva en texto plano. */
public abstract class Persona {
    private final String id;
    private final String nombre;
    private final String correo;
    private final byte[] salt = new byte[16];
    private final byte[] password;

    /** Construye una identidad con credenciales privadas y sal aleatoria. */
    protected Persona(String id, String nombre, String correo, String password) {
        this.id = texto(id, "id");
        this.nombre = texto(nombre, "nombre");
        this.correo = texto(correo, "correo").toLowerCase(Locale.ROOT);
        if (!this.correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Correo inválido");
        if (password == null || password.length() < 8)
            throw new IllegalArgumentException("Contraseña: mínimo 8 caracteres");
        new SecureRandom().nextBytes(salt);
        this.password = derivar(password);
    }
    /** Reconstruye una identidad desde credenciales derivadas persistidas (nunca contraseña plana). */
    protected Persona(String id, String nombre, String correo, String credencialPersistida, boolean restaurar) {
        this.id = texto(id, "id");
        this.nombre = texto(nombre, "nombre");
        this.correo = texto(correo, "correo").toLowerCase(Locale.ROOT);
        if (!this.correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Correo inválido");
        try {
            String[] partes = credencialPersistida.split(":", -1);
            if (partes.length != 2) throw new IllegalArgumentException();
            byte[] s = Base64.getDecoder().decode(partes[0]);
            byte[] h = Base64.getDecoder().decode(partes[1]);
            if (s.length != 16 || h.length != 32) throw new IllegalArgumentException();
            System.arraycopy(s, 0, this.salt, 0, this.salt.length);
            this.password = h;
        } catch (RuntimeException ex) { throw new IllegalArgumentException("Credenciales persistidas inválidas", ex); }
    }
    /** Exporta únicamente sal y hash PBKDF2 para persistencia segura. */
    public final String credencialPersistida() {
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(password);
    }
    /** Valida los textos obligatorios del dominio. */
    public static String texto(String value, String campo) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(campo + " obligatorio");
        return value.trim();
    }
    private byte[] derivar(String value) {
        PBEKeySpec spec = new PBEKeySpec(value.toCharArray(), salt, 120000, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException ex) { throw new IllegalStateException("No hay proveedor criptográfico", ex); }
        finally { spec.clearPassword(); }
    }
    /** Compara credenciales mediante un hash derivado y comparación constante. */
    public final boolean autenticar(String candidate) {
        return candidate != null && MessageDigest.isEqual(password, derivar(candidate));
    }
    /** Identificador único e inmutable. */
    public final String getId() { return id; }
    /** Nombre para presentación. */
    public final String getNombre() { return nombre; }
    /** Correo normalizado utilizado para el acceso. */
    public final String getCorreo() { return correo; }
    /** Permite presentar distintos roles mediante polimorfismo. */
    public abstract String getRol();
}
