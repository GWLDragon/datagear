/*
 * Copyright (c) 2018 datagear.org. All Rights Reserved.
 */

package org.datagear.web.sqlpad;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.cometd.bayeux.server.ServerChannel;
import org.datagear.connection.ConnectionSource;
import org.datagear.connection.ConnectionSourceException;
import org.datagear.management.domain.Schema;
import org.datagear.management.service.SqlHistoryService;
import org.datagear.management.util.SchemaConnectionSupport;
import org.datagear.persistence.support.PersistenceSupport;
import org.datagear.persistence.support.SqlSelectManager;
import org.datagear.persistence.support.SqlSelectResult;
import org.datagear.util.JdbcUtil;
import org.datagear.util.SqlScriptParser.SqlStatement;
import org.datagear.web.controller.SqlpadController.SqlpadFileDirectory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

/**
 * SQL工作台执行SQL服务。
 * 
 * @author datagear@163.com
 *
 */
public class SqlpadExecutionService extends PersistenceSupport
{
	private ConnectionSource connectionSource;

	private MessageSource messageSource;

	private SqlpadCometdService sqlpadCometdService;

	private SqlHistoryService sqlHistoryService;

	private SqlSelectManager sqlSelectManager;

	private SqlPermissionChecker sqlPermissionChecker = new SqlPermissionChecker();

	private SchemaConnectionSupport schemaConnectionSupport = new SchemaConnectionSupport();

	private ExecutorService _executorService = Executors.newCachedThreadPool();

	private ConcurrentMap<String, SqlpadExecutionRunnable> _sqlpadExecutionRunnableMap = new ConcurrentHashMap<>();

	public SqlpadExecutionService()
	{
		super();
	}

	public SqlpadExecutionService(ConnectionSource connectionSource, MessageSource messageSource,
			SqlpadCometdService sqlpadCometdService, SqlHistoryService sqlHistoryService,
			SqlSelectManager sqlSelectManager)
	{
		super();
		this.connectionSource = connectionSource;
		this.messageSource = messageSource;
		this.sqlpadCometdService = sqlpadCometdService;
		this.sqlHistoryService = sqlHistoryService;
		this.sqlSelectManager = sqlSelectManager;
	}

	public ConnectionSource getConnectionSource()
	{
		return connectionSource;
	}

	public void setConnectionSource(ConnectionSource connectionSource)
	{
		this.connectionSource = connectionSource;
	}

	public MessageSource getMessageSource()
	{
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource)
	{
		this.messageSource = messageSource;
	}

	public SqlpadCometdService getSqlpadCometdService()
	{
		return sqlpadCometdService;
	}

	public void setSqlpadCometdService(SqlpadCometdService sqlpadCometdService)
	{
		this.sqlpadCometdService = sqlpadCometdService;
	}

	public SqlHistoryService getSqlHistoryService()
	{
		return sqlHistoryService;
	}

	public void setSqlHistoryService(SqlHistoryService sqlHistoryService)
	{
		this.sqlHistoryService = sqlHistoryService;
	}

	public SqlSelectManager getSqlSelectManager()
	{
		return sqlSelectManager;
	}

	public void setSqlSelectManager(SqlSelectManager sqlSelectManager)
	{
		this.sqlSelectManager = sqlSelectManager;
	}

	public SqlPermissionChecker getSqlPermissionChecker()
	{
		return sqlPermissionChecker;
	}

	public void setSqlPermissionChecker(SqlPermissionChecker sqlPermissionChecker)
	{
		this.sqlPermissionChecker = sqlPermissionChecker;
	}

	public SchemaConnectionSupport getSchemaConnectionSupport()
	{
		return schemaConnectionSupport;
	}

	public void setSchemaConnectionSupport(SchemaConnectionSupport schemaConnectionSupport)
	{
		this.schemaConnectionSupport = schemaConnectionSupport;
	}

	/**
	 * 提交SQL执行。
	 * 
	 * @param submit
	 * @return
	 */
	public boolean submit(SqlpadExecutionSubmit submit)
	{
		String sqlpadChannelId = getSqlpadChannelId(submit.getSqlpadId());

		SqlpadExecutionRunnable sqlpadExecutionRunnable = new SqlpadExecutionRunnable(submit, sqlpadChannelId,
				sqlpadCometdService);

		SqlpadExecutionRunnable old = this._sqlpadExecutionRunnableMap.putIfAbsent(submit.getSqlpadId(),
				sqlpadExecutionRunnable);

		if (old != null)
			return false;

		sqlpadExecutionRunnable.init();

		this._executorService.submit(sqlpadExecutionRunnable);

		return true;
	}

	/**
	 * 发送SQL命令。
	 * 
	 * @param sqlpadId
	 * @param sqlCommand
	 * @return
	 */
	public boolean command(String sqlpadId, SqlCommand sqlCommand)
	{
		SqlpadExecutionRunnable sqlpadExecutionRunnable = this._sqlpadExecutionRunnableMap.get(sqlpadId);

		if (sqlpadExecutionRunnable == null)
			return false;

		sqlpadExecutionRunnable.setSqlCommand(sqlCommand);

		return true;
	}

	/**
	 * 关闭。
	 */
	public void shutdown()
	{
		this._executorService.shutdown();
	}

	/**
	 * 获取指定SQL工作台ID对应的cometd通道ID。
	 * 
	 * @param sqlpadId
	 * @return
	 */
	public String getSqlpadChannelId(String sqlpadId)
	{
		return "/sqlpad/channel/" + sqlpadId;
	}

	/**
	 * 获取指定{@linkplain Schema}的{@linkplain Connection}。
	 * 
	 * @param schema
	 * @return
	 * @throws ConnectionSourceException
	 */
	protected Connection getSchemaConnection(Schema schema) throws ConnectionSourceException
	{
		return this.schemaConnectionSupport.getSchemaConnection(this.connectionSource, schema);
	}

	/**
	 * 获取I18N消息内容。
	 * <p>
	 * 如果找不到对应消息码的消息，则返回<code>"???[code]???"<code>（例如：{@code "???error???"}）。
	 * </p>
	 * 
	 * @param locale
	 * @param code
	 * @param args
	 * @return
	 */
	protected String getMessage(Locale locale, String code, Object... args)
	{
		try
		{
			return this.messageSource.getMessage(code, args, locale);
		}
		catch (NoSuchMessageException e)
		{
			return "???" + code + "???";
		}
	}

	/**
	 * 用于执行SQL的{@linkplain Runnable}。
	 * 
	 * @author datagear@163.com
	 *
	 */
	protected class SqlpadExecutionRunnable extends SqlpadExecutionSubmit implements Runnable
	{
		private String sqlpadChannelId;

		private SqlpadCometdService sqlpadCometdService;

		/** 发送给此Runnable的SQL命令 */
		private volatile SqlCommand sqlCommand;

		private ServerChannel _sqlpadServerChannel;

		public SqlpadExecutionRunnable()
		{
			super();
		}

		public SqlpadExecutionRunnable(SqlpadExecutionSubmit submit, String sqlpadChannelId,
				SqlpadCometdService sqlpadCometdService)
		{
			super(submit);
			this.sqlpadChannelId = sqlpadChannelId;
			this.sqlpadCometdService = sqlpadCometdService;
		}

		public String getSqlpadChannelId()
		{
			return sqlpadChannelId;
		}

		public void setSqlpadChannelId(String sqlpadChannelId)
		{
			this.sqlpadChannelId = sqlpadChannelId;
		}

		public SqlpadCometdService getSqlpadCometdService()
		{
			return sqlpadCometdService;
		}

		public void setSqlpadCometdService(SqlpadCometdService sqlpadCometdService)
		{
			this.sqlpadCometdService = sqlpadCometdService;
		}

		public SqlCommand getSqlCommand()
		{
			return sqlCommand;
		}

		public void setSqlCommand(SqlCommand sqlCommand)
		{
			this.sqlCommand = sqlCommand;
		}

		/**
		 * 初始化。
		 * <p>
		 * 此方法应该在{@linkplain #run()}之前调用。
		 * </p>
		 */
		public void init()
		{
			this._sqlpadServerChannel = this.sqlpadCometdService.getChannelWithCreation(this.sqlpadChannelId);
		}

		@Override
		public void run()
		{
			Connection cn = null;
			Statement st = null;

			this.sqlpadCometdService.sendStartMessage(this._sqlpadServerChannel, getSqlStatements().size());

			try
			{
				cn = getSchemaConnection(getSchema());
				JdbcUtil.setAutoCommitIfSupports(cn, false);
				JdbcUtil.setReadonlyIfSupports(cn, false);
				st = createStatement(cn);
			}
			catch (Throwable t)
			{
				this.sqlpadCometdService.sendExceptionMessage(_sqlpadServerChannel, t,
						getMessage(getLocale(), "sqlpad.executionConnectionException"), false);

				this.sqlpadCometdService.sendFinishMessage(this._sqlpadServerChannel);

				_sqlpadExecutionRunnableMap.remove(getSqlpadId());

				return;
			}

			long startTime = System.currentTimeMillis();
			int totalCount = getSqlStatements().size();
			SQLExecutionStat sqlExecutionStat = new SQLExecutionStat(totalCount);
			SqlpadFileDirectory sqlpadFileDirectory = SqlpadFileDirectory.valueOf(getSqlpadFileDirectory());

			List<String> sqlHistories = new ArrayList<>();

			try
			{
				for (int i = 0; i < totalCount; i++)
				{
					if (handleSqlCommandInExecution(cn, true, sqlExecutionStat))
						break;

					SqlStatement sqlStatement = getSqlStatements().get(i);

					if (!SqlpadExecutionService.this.sqlPermissionChecker.hasPermission(getUser(), getSchema(),
							sqlStatement))
					{
						this.sqlpadCometdService.sendSqlExceptionMessage(_sqlpadServerChannel, sqlStatement, i,
								getMessage(getLocale(), "sqlpad.executionSQLPermissionDenied"));

						sqlExecutionStat.increaseExceptionCount();
					}
					else
					{
						try
						{
							execute(sqlExecutionStat, sqlpadFileDirectory, cn, st, sqlStatement, i);
							sqlExecutionStat.increaseSuccessCount();

							sqlHistories.add(sqlStatement.getSql());
						}
						catch (SQLException e)
						{
							sqlExecutionStat.increaseExceptionCount();

							this.sqlpadCometdService.sendSqlExceptionMessage(_sqlpadServerChannel, sqlStatement, i, e,
									getMessage(getLocale(), "sqlpad.executionSQLException", e.getMessage()));

							if (ExceptionHandleMode.IGNORE.equals(getExceptionHandleMode()))
								;
							else
							{
								break;
							}
						}
					}
				}

				if (SqlCommand.STOP.equals(this.sqlCommand))
					;
				else
				{
					if (CommitMode.AUTO.equals(getCommitMode()))
					{
						if (sqlExecutionStat.getExceptionCount() > 0
								&& ExceptionHandleMode.ROLLBACK.equals(getExceptionHandleMode()))
							this.sqlCommand = SqlCommand.ROLLBACK;
						else
							this.sqlCommand = SqlCommand.COMMIT;
					}

					waitForCommitOrRollbackCommand(cn, sqlExecutionStat);
				}
			}
			catch (Throwable t)
			{
				this.sqlpadCometdService.sendExceptionMessage(_sqlpadServerChannel, t,
						getMessage(getLocale(), "sqlpad.executionErrorOccure"), true);
			}
			finally
			{
				JdbcUtil.closeStatement(st);
				JdbcUtil.closeConnection(cn);

				sqlExecutionStat.setTaskDuration(System.currentTimeMillis() - startTime);

				this.sqlpadCometdService.sendFinishMessage(this._sqlpadServerChannel, sqlExecutionStat);

				_sqlpadExecutionRunnableMap.remove(getSqlpadId());
			}

			if (!sqlHistories.isEmpty())
				SqlpadExecutionService.this.sqlHistoryService.addForRemain(getSchema().getId(), getUser().getId(),
						sqlHistories);
		}

		/**
		 * 处理执行时命令。
		 * 
		 * @param cn
		 * @param sendMessageIfPause
		 * @param sqlExecutionStat
		 * @return true 退出执行循环；false 不退出执行循环。
		 * @throws SQLException
		 * @throws InterruptedException
		 */
		protected boolean handleSqlCommandInExecution(Connection cn, boolean sendMessageIfPause,
				SQLExecutionStat sqlExecutionStat) throws SQLException, InterruptedException
		{
			boolean breakLoop = false;

			boolean hasPaused = false;

			if (SqlCommand.PAUSE.equals(this.sqlCommand))
			{
				hasPaused = true;

				if (sendMessageIfPause)
					sendSqlCommandMessage(this.sqlCommand, getOverTimeThreashold());

				long waitStartTime = System.currentTimeMillis();

				while (SqlCommand.PAUSE.equals(this.sqlCommand)
						&& (System.currentTimeMillis() - waitStartTime) <= getOverTimeThreashold() * 60 * 1000)
					sleepForSqlCommand();

				// 暂停超时
				if (SqlCommand.PAUSE.equals(this.sqlCommand))
				{
					this.sqlpadCometdService.sendTextMessage(this._sqlpadServerChannel,
							getMessage(getLocale(), "sqlpad.pauseOverTime"));

					this.sqlCommand = SqlCommand.RESUME;
				}
			}

			if (SqlCommand.RESUME.equals(this.sqlCommand))
			{
				sendSqlCommandMessage(this.sqlCommand);

				this.sqlCommand = null;
			}
			else if (SqlCommand.STOP.equals(this.sqlCommand))
			{
				cn.rollback();
				sendSqlCommandMessage(this.sqlCommand);

				breakLoop = true;
			}
			else if (SqlCommand.COMMIT.equals(this.sqlCommand))
			{
				cn.commit();
				sendSqlCommandMessage(this.sqlCommand);

				// 提交操作不打断暂停
				if (hasPaused)
				{
					this.sqlCommand = SqlCommand.PAUSE;
					breakLoop = handleSqlCommandInExecution(cn, false, sqlExecutionStat);
				}
				else
					this.sqlCommand = null;
			}
			else if (SqlCommand.ROLLBACK.equals(this.sqlCommand))
			{
				cn.rollback();
				sendSqlCommandMessage(this.sqlCommand);

				// 回滚操作不打断暂停
				if (hasPaused)
				{
					this.sqlCommand = SqlCommand.PAUSE;
					breakLoop = handleSqlCommandInExecution(cn, false, sqlExecutionStat);
				}
				else
					this.sqlCommand = null;
			}

			return breakLoop;
		}

		/**
		 * 等待执行提交或者是回滚命令。
		 * 
		 * @param cn
		 * @param sqlExecutionStat
		 * @throws SQLException
		 * @throws InterruptedException
		 */
		protected void waitForCommitOrRollbackCommand(Connection cn, SQLExecutionStat sqlExecutionStat)
				throws SQLException, InterruptedException
		{
			boolean sendWatingMessage = false;

			long waitStartTime = System.currentTimeMillis();

			while (!SqlCommand.COMMIT.equals(this.sqlCommand) && !SqlCommand.ROLLBACK.equals(this.sqlCommand)
					&& (System.currentTimeMillis() - waitStartTime) <= getOverTimeThreashold() * 60 * 1000)
			{
				if (!sendWatingMessage)
				{
					this.sqlpadCometdService.sendTextMessage(this._sqlpadServerChannel,
							getMessage(getLocale(), "sqlpad.waitingForCommitOrRollback", getOverTimeThreashold()),
							"message-content-highlight", sqlExecutionStat);

					sendWatingMessage = true;
				}

				sleepForSqlCommand();
			}

			// 等待超时
			if (!SqlCommand.COMMIT.equals(this.sqlCommand) && !SqlCommand.ROLLBACK.equals(this.sqlCommand))
			{
				this.sqlpadCometdService.sendTextMessage(this._sqlpadServerChannel,
						getMessage(getLocale(), "sqlpad.waitOverTime"));

				this.sqlCommand = (sqlExecutionStat.getExceptionCount() > 0 ? SqlCommand.ROLLBACK : SqlCommand.COMMIT);
			}

			if (SqlCommand.COMMIT.equals(this.sqlCommand))
			{
				JdbcUtil.commitIfSupports(cn);
				sendSqlCommandMessage(this.sqlCommand);

				this.sqlCommand = null;
			}
			else if (SqlCommand.ROLLBACK.equals(this.sqlCommand))
			{
				JdbcUtil.rollbackIfSupports(cn);
				sendSqlCommandMessage(this.sqlCommand);

				this.sqlCommand = null;
			}
		}

		/**
		 * 执行SQL，出现异常时应该抛出{@linkplain SQLException}。
		 * 
		 * @param sqlExecutionStat
		 * @param sqlpadFileDirectory
		 * @param cn
		 * @param st
		 * @param sqlStatement
		 * @param sqlStatementIndex
		 * @throws SQLException
		 */
		protected void execute(SQLExecutionStat sqlExecutionStat, SqlpadFileDirectory sqlpadFileDirectory,
				Connection cn, Statement st, SqlStatement sqlStatement, int sqlStatementIndex) throws SQLException
		{
			long startTime = System.currentTimeMillis();

			// 禁用插入文件功能，因为没有应用场景
			// String sql =
			// sqlpadFileDirectory.replaceNameToAbsolutePath(sqlStatement.getSql());
			String sql = sqlStatement.getSql();

			boolean isResultSet = st.execute(sql);

			sqlExecutionStat.increaseSqlDuration(System.currentTimeMillis() - startTime);

			// 查询操作
			if (isResultSet)
			{
				ResultSet rs = st.getResultSet();

				SqlSelectResult sqlSelectResult = SqlpadExecutionService.this.sqlSelectManager.select(cn, sql, rs, 1,
						getResultsetFetchSize(), getResultsetRowMapper());

				this.sqlpadCometdService.sendSqlSuccessMessage(this._sqlpadServerChannel, sqlStatement,
						sqlStatementIndex, sqlSelectResult);
			}
			else
			{
				int updateCount = st.getUpdateCount();

				// 更新操作
				if (updateCount > -1)
				{
					this.sqlpadCometdService.sendSqlSuccessMessage(this._sqlpadServerChannel, sqlStatement,
							sqlStatementIndex, updateCount);
				}
				// 其他操作
				else
				{
					this.sqlpadCometdService.sendSqlSuccessMessage(this._sqlpadServerChannel, sqlStatement,
							sqlStatementIndex);
				}
			}
		}

		/**
		 * 发送命令已执行消息。
		 * 
		 * @param sqlCommand
		 * @param messageArgs
		 */
		protected void sendSqlCommandMessage(SqlCommand sqlCommand, Object... messageArgs)
		{
			String messageKey = "sqlpad.SqlCommand." + sqlCommand.toString() + ".ok";

			this.sqlpadCometdService.sendSqlCommandMessage(this._sqlpadServerChannel, sqlCommand,
					getMessage(getLocale(), messageKey, messageArgs));
		}

		/**
		 * 创建执行SQL语句所需要的{@linkplain Statement}。
		 * 
		 * @param cn
		 * @return
		 * @throws SQLException
		 */
		protected Statement createStatement(Connection cn) throws SQLException
		{
			// 某些查询SQL语句并不支持ResultSet.TYPE_SCROLL_*（比如SQLServer的聚集列存储索引），
			// 而这里调用的结果集都是从第一行开始，不会用到ResultSet.TYPE_SCROLL_*特性，
			// 因而采用ResultSet.TYPE_FORWARD_ONLY，避免遇到上述情况而抛出异常
			Statement st = createUpdateStatement(cn);
			JdbcUtil.setFetchSizeIfSupports(st, getResultsetFetchSize());

			return st;
		}

		/**
		 * 睡眠等待SQL命令。
		 * 
		 * @throws InterruptedException
		 */
		protected void sleepForSqlCommand() throws InterruptedException
		{
			Thread.sleep(10);
		}
	}

	/**
	 * SQL执行统计信息。
	 * 
	 * @author datagear@163.com
	 *
	 */
	public static class SQLExecutionStat implements Serializable
	{
		private static final long serialVersionUID = 1L;

		/** 总计SQL语句数 */
		private int totalCount;

		/** 执行成功数 */
		private int successCount = 0;

		/** 执行失败数 */
		private int exceptionCount = 0;

		/** SQL执行持续毫秒数，-1表示未记录 */
		private long sqlDuration = -1;

		/** 任务执行持续毫秒数，-1表示未记录 */
		private long taskDuration = -1;

		public SQLExecutionStat()
		{
			super();
		}

		public SQLExecutionStat(int totalCount)
		{
			super();
			this.totalCount = totalCount;
		}

		public SQLExecutionStat(int totalCount, int successCount, int exceptionCount, long sqlDuration)
		{
			super();
			this.totalCount = totalCount;
			this.successCount = successCount;
			this.exceptionCount = exceptionCount;
			this.sqlDuration = sqlDuration;
		}

		public int getTotalCount()
		{
			return totalCount;
		}

		public void setTotalCount(int totalCount)
		{
			this.totalCount = totalCount;
		}

		public int getSuccessCount()
		{
			return successCount;
		}

		public void setSuccessCount(int successCount)
		{
			this.successCount = successCount;
		}

		public int getExceptionCount()
		{
			return exceptionCount;
		}

		public void setExceptionCount(int exceptionCount)
		{
			this.exceptionCount = exceptionCount;
		}

		public long getSqlDuration()
		{
			return sqlDuration;
		}

		public void setSqlDuration(long sqlDuration)
		{
			this.sqlDuration = sqlDuration;
		}

		public long getTaskDuration()
		{
			return taskDuration;
		}

		public void setTaskDuration(long taskDuration)
		{
			this.taskDuration = taskDuration;
		}

		public int getAbortCount()
		{
			return this.totalCount - this.successCount - this.exceptionCount;
		}

		public void increaseSuccessCount()
		{
			this.successCount += 1;
		}

		public void increaseExceptionCount()
		{
			this.exceptionCount += 1;
		}

		public void increaseSqlDuration(long increment)
		{
			if (this.sqlDuration < 0)
				this.sqlDuration = 0;

			this.sqlDuration += increment;
		}

		public void increaseTaskDuration(long increment)
		{
			if (this.taskDuration < 0)
				this.taskDuration = 0;

			this.taskDuration += increment;
		}
	}

	/**
	 * 提交模式。
	 * 
	 * @author datagear@163.com
	 *
	 */
	public static enum CommitMode
	{
		AUTO,

		MANUAL
	}

	/**
	 * 错误处理模式。
	 * 
	 * @author datagear@163.com
	 *
	 */
	public static enum ExceptionHandleMode
	{
		ABORT,

		IGNORE,

		ROLLBACK
	}

	/**
	 * SQL执行命令。
	 * 
	 * @author datagear@163.com
	 *
	 */
	public static enum SqlCommand
	{
		/** 提交 */
		COMMIT,

		/** 回滚 */
		ROLLBACK,

		/** 暂停 */
		PAUSE,

		/** 继续 */
		RESUME,

		/** 停止 */
		STOP
	}

	/**
	 * SQL执行结果类型。
	 * 
	 * @author datagear@163.com
	 *
	 */
	public static enum SqlResultType
	{
		/** 结果集 */
		RESULT_SET,

		/** 更新数目 */
		UPDATE_COUNT,

		/** 无结果 */
		NONE
	}
}
