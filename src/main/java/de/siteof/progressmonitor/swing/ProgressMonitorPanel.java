package de.siteof.progressmonitor.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.siteof.progressmonitor.IProgressCallback;
import de.siteof.progressmonitor.IProgressMonitor;
import de.siteof.progressmonitor.IProgressMonitorFactory;
import de.siteof.util.swing.SwingUtil;

public class ProgressMonitorPanel extends JPanel implements IProgressMonitorFactory {

	private class ProgressMonitorTableModel extends AbstractTableModel implements TableModel {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;


		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
				case 0:
					return "Name";
				case 1:
					return "Progress";
			}
			return "unknown " + columnIndex;
		}

		@Override
		public int getRowCount() {
			synchronized (progressList) {
				return progressList.size();
			}
		}

		private String formatValue(long number) {
			NumberFormat format = NumberFormat.getNumberInstance();
			return format.format(number);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			ProgressMonitor progressMonitor	= null;
			synchronized (progressList) {
				if ((rowIndex >= 0) && (rowIndex < progressList.size())) {
					progressMonitor	= (ProgressMonitor) progressList.get(rowIndex);
				}
			}
			Object result	= null;
			if (progressMonitor != null) {
				ProgressEntry entry	= progressMonitor.getEntry(null);
				switch (columnIndex) {
					case 0:
						result	= entry.getTaskName();
						break;
					case 1:
						if (entry.getTotalWork() == IProgressMonitor.UNKNOWN) {
							if (entry.getWorked() == 0) {
								result	= "";
							} else {
								result	= formatValue(entry.getWorked());
							}
						} else {
							result	= formatValue(entry.getWorked()) + "/" + formatValue(entry.getTotalWork());
						}
						String subTask = entry.getSubTaskName();
						if ((subTask != null) && (subTask.length() > 0)) {
							result = result + " " + subTask;
						}
						if (entry.isCanceled()) {
							result	= result.toString() + " (cancelled)";
						} else if (entry.isDone()) {
							result	= result.toString() + " (done)";
						}
						break;
				}
			}
			return result;
		}

	}


	private static class ProgressEntry {

		private String taskName;
		private String subTaskName;
		private int totalWork;
		private int worked;
		private boolean canceled;
		private boolean done;
		/**
		 * @return the canceled
		 */
		public boolean isCanceled() {
			return canceled;
		}
		/**
		 * @param canceled the canceled to set
		 */
		public void setCanceled(boolean canceled) {
			this.canceled = canceled;
		}
		/**
		 * @return the subTaskName
		 */
		public String getSubTaskName() {
			return subTaskName;
		}
		/**
		 * @param subTaskName the subTaskName to set
		 */
		public void setSubTaskName(String subTaskName) {
			this.subTaskName = subTaskName;
		}
		/**
		 * @return the taskName
		 */
		public String getTaskName() {
			return taskName;
		}
		/**
		 * @param taskName the taskName to set
		 */
		public void setTaskName(String taskName) {
			this.taskName = taskName;
		}
		/**
		 * @return the totalWork
		 */
		public int getTotalWork() {
			return totalWork;
		}
		/**
		 * @param totalWork the totalWork to set
		 */
		public void setTotalWork(int totalWork) {
			this.totalWork = totalWork;
		}
		/**
		 * @return the worked
		 */
		public int getWorked() {
			return worked;
		}
		/**
		 * @param worked the worked to set
		 */
		public void setWorked(int worked) {
			this.worked = worked;
		}
		/**
		 * @return the done
		 */
		public boolean isDone() {
			return done;
		}
		/**
		 * @param done the done to set
		 */
		public void setDone(boolean done) {
			this.done = done;
		}

	}

	private static int idCounter	= 1;

	private static class ProgressMonitor implements IProgressMonitor {


		private final int id	= (idCounter++);
		private boolean added			= false;
		private boolean done			= false;
		private boolean canceled		= false;
		private String taskName;
		private String subTaskName;
		private int totalWork;
		private int worked;

		private final ProgressMonitorPanel parent;
		private final IProgressCallback callback;

		private static final Log log	= LogFactory.getLog(ProgressMonitorPanel.class);


		private ProgressMonitor(ProgressMonitorPanel parent, IProgressCallback callback) {
			this.parent	= parent;
			this.callback = callback;
		}


		private void checkNotDone() {
			if (done) {
				throw new IllegalStateException("already done");
			}
		}

		public ProgressEntry getEntry(ProgressEntry entry) {
			if (entry == null) {
				entry	= new ProgressEntry();
			}
			synchronized (this) {
				entry.setTaskName(taskName);
				entry.setSubTaskName(subTaskName);
				entry.setCanceled(canceled);
				entry.setDone(done);
				entry.setTotalWork(totalWork);
				entry.setWorked(worked);
			}
			return entry;
		}

		private void update() {
			if (done) {
				SwingUtil.invokeLater(new Runnable() {
					@Override
					public void run() {
						int index;
						List<ProgressMonitor> progressList		= parent.progressList;
						ProgressMonitorTableModel tableModel	= parent.tableModel;
						synchronized (progressList) {
							index	= progressList.indexOf(ProgressMonitor.this);
							if (index >= 0) {
								progressList.remove(index);
								tableModel.fireTableRowsDeleted(index, index);
								tableModel.fireTableStructureChanged();
								tableModel.fireTableDataChanged();
								parent.table.repaint();
								log.debug("removed");
							} else {
								log.debug("not found, id=" + id + ", list=" + progressList);
							}
						}
					}});
				log.debug("removed1");
			} else if (!added) {
				List<ProgressMonitor> progressList				= parent.progressList;
				synchronized (progressList) {
					progressList.add(0, this);
				}
				added	= true;
				SwingUtil.invokeLater(new Runnable() {
					@Override
					public void run() {
						int index;
						List<ProgressMonitor> progressList		= parent.progressList;
						ProgressMonitorTableModel tableModel	= parent.tableModel;
						synchronized (progressList) {
							index	= progressList.indexOf(ProgressMonitor.this);
							if (index >= 0) {
								tableModel.fireTableRowsInserted(index, index);
//								tableModel.fireTableStructureChanged();
//								tableModel.fireTableDataChanged();
								parent.table.repaint();
								log.debug("added, id=" + id);
							} else {
								log.debug("not found, id=" + id + ", list=" + progressList);
							}
						}
					}});
				log.debug("added1, id=" + id);
			} else {
				SwingUtil.invokeLater(new Runnable() {
					@Override
					public void run() {
						int index;
						List<ProgressMonitor> progressList		= parent.progressList;
						ProgressMonitorTableModel tableModel	= parent.tableModel;
						synchronized (progressList) {
							index	= progressList.indexOf(ProgressMonitor.this);
							if (index >= 0) {
								tableModel.fireTableRowsUpdated(index, index);
//								tableModel.fireTableStructureChanged();
//								tableModel.fireTableDataChanged();
								parent.table.repaint();
								log.debug("updated, id=" + id);
							} else {
								log.debug("not found, id=" + id + ", list=" + progressList);
							}
						}
					}});
				log.debug("updated1, id=" + id);
			}
		}

		@Override
		public void beginTask(String name, int totalWork) {
			checkNotDone();
			this.taskName		= name;
			this.totalWork		= totalWork;
			this.subTaskName	= null;
			this.worked		= 0;
			update();

		}

		@Override
		public void done() {
			done	= true;
			update();
		}

		@Override
		public void internalWorked(double work) {
			checkNotDone();
		}

		@Override
		public boolean isCanceled() {
			return canceled;
		}

		@Override
		public void setCanceled(boolean value) {
			checkNotDone();
			canceled	= value;
			update();
		}

		@Override
		public void setTaskName(String name) {
			checkNotDone();
			taskName	= name;
			update();
		}

		@Override
		public void subTask(String name) {
			checkNotDone();
			subTaskName	= name;
			update();
		}

		@Override
		public void worked(int work) {
			checkNotDone();
			worked	+= work;
			update();
		}



		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "ProgressMonitor(" + this.id + ")";
		}

	}


	private class CancelAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		public CancelAction() {
			super("Cancel");
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			if ((selectedProgressMonitor != null) &&
					(selectedProgressMonitor.callback != null)) {
				selectedProgressMonitor.callback.cancel();
			}
		}

		/* (non-Javadoc)
		 * @see javax.swing.AbstractAction#isEnabled()
		 */
		@Override
		public boolean isEnabled() {
			return ((selectedProgressMonitor != null) &&
					(selectedProgressMonitor.callback != null) &&
					(selectedProgressMonitor.callback.canCancel()));
		}

	}



	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final JTable table;
	private ProgressMonitorTableModel tableModel;
	private final List<ProgressMonitor> progressList	= new ArrayList<ProgressMonitor>();
	private ProgressMonitor selectedProgressMonitor;
//	private JPopupMenu menu = new JPopupMenu();
//	private CancelAction cancelAction;

	public ProgressMonitorPanel() {
		setLayout(new BorderLayout());
		table	= new JTable(tableModel = new ProgressMonitorTableModel());
		JScrollPane tableScrollPane	= new JScrollPane(table);
		this.add(tableScrollPane, BorderLayout.CENTER);

		table.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent event) {
				if ((event.getClickCount() == 1) && (event.getButton() == MouseEvent.BUTTON3)) {
//				if (event.isPopupTrigger()) {
					selectedProgressMonitor = null;
					synchronized (progressList) {
						int selectedRow = table.getSelectedRow();
						if ((selectedRow >= 0) && (selectedRow < progressList.size())) {
							selectedProgressMonitor	= progressList.get(selectedRow);
						}
					}
//					cancelAction.setEnabled(cancelAction.isEnabled());
					JPopupMenu menu = new JPopupMenu();
					menu.add(new CancelAction());
					menu.show(table, event.getX(), event.getY());
				}
			}

			@Override
			public void mouseEntered(MouseEvent event) {
			}

			@Override
			public void mouseExited(MouseEvent event) {
			}

			@Override
			public void mousePressed(MouseEvent event) {
			}

			@Override
			public void mouseReleased(MouseEvent event) {
			}});

//		IProgressMonitor dummyProgressMonitor;
//		dummyProgressMonitor	= createProgressMonitor();
//		dummyProgressMonitor.beginTask("dummy task", 10);
//		dummyProgressMonitor.worked(4);
	}

	@Override
	public IProgressMonitor createProgressMonitor() {
		return new ProgressMonitor(this, null);
	}

	@Override
	public IProgressMonitor createProgressMonitor(IProgressCallback callback) {
		return new ProgressMonitor(this, callback);
	}

}
